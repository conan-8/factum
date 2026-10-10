//
//  StorageService.swift
//  Pigeon
//
//  Supabase Storage for video, thumbnail, and avatar uploads
//

import Foundation
import UIKit
import AVFoundation
import Supabase

final class StorageService: Sendable {
    static let shared = StorageService()
    
    /// Maximum file size for Supabase Storage uploads (50 MB).
    private let maxUploadBytes = 50 * 1024 * 1024
    
    private init() {}
    
    // MARK: - Video Upload
    
    /// Upload a timelapse video to Supabase Storage.
    /// If the file exceeds 50 MB, it is re-compressed to a smaller size first.
    /// Returns the public URL string.
    func uploadVideo(localURL: URL, userUID: String, timelapseID: String, onProgress: ((Double) -> Void)? = nil) async throws -> String {
        // Verify the file exists before attempting upload
        guard FileManager.default.fileExists(atPath: localURL.path) else {
            print("[SYNC] Video file NOT FOUND at: \(localURL.path)")
            throw StorageError.uploadFailed
        }
        
        let fileSize = (try? FileManager.default.attributesOfItem(atPath: localURL.path)[.size] as? Int) ?? 0
        print("[SYNC] Video file size: \(fileSize / 1024)KB at path: \(localURL.lastPathComponent)")
        
        // Re-compress if the video exceeds the storage limit
        let uploadURL: URL
        if fileSize > maxUploadBytes {
            print("[SYNC] Video exceeds \(maxUploadBytes / 1024 / 1024)MB limit — re-compressing...")
            if let compressed = try await compressVideo(at: localURL) {
                let compressedSize = (try? FileManager.default.attributesOfItem(atPath: compressed.path)[.size] as? Int) ?? 0
                print("[SYNC] Re-compressed: \(fileSize / 1024)KB → \(compressedSize / 1024)KB")
                uploadURL = compressed
            } else {
                print("[SYNC] Re-compression failed — uploading original")
                uploadURL = localURL
            }
        } else {
            uploadURL = localURL
        }
        
        let videoData = try Data(contentsOf: uploadURL)
        let path = "\(userUID)/\(timelapseID).mp4"
        
        print("[SYNC] Uploading video: bucket=timelapses, path=\(path), size=\(videoData.count / 1024)KB, userUID=\(userUID.prefix(8))...")
        
        do {
            try await supabase.storage.from("timelapses").upload(
                path,
                data: videoData,
                options: FileOptions(contentType: "video/mp4", upsert: true)
            )
        } catch {
            print("[SYNC] ❌ Storage upload error: \(error)")
            print("[SYNC] ❌ Error type: \(type(of: error))")
            throw error
        }
        
        // Clean up temporary compressed file
        if uploadURL != localURL {
            try? FileManager.default.removeItem(at: uploadURL)
        }
        
        let publicURL = try supabase.storage.from("timelapses").getPublicURL(path: path)
        print("[SYNC] ✅ Video public URL: \(publicURL.absoluteString)")
        return publicURL.absoluteString
    }
    
    /// Re-encode a video at medium quality to reduce file size below the upload limit.
    private func compressVideo(at sourceURL: URL) async throws -> URL? {
        let asset = AVURLAsset(url: sourceURL)
        let outputURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("compressed_\(UUID().uuidString).mp4")
        
        guard let exportSession = AVAssetExportSession(
            asset: asset, presetName: AVAssetExportPresetMediumQuality
        ) else {
            return nil
        }
        
        exportSession.outputURL = outputURL
        exportSession.outputFileType = .mp4
        exportSession.shouldOptimizeForNetworkUse = true
        
        await exportSession.export()
        
        guard exportSession.status == .completed else {
            print("[SYNC] Re-compression export failed: \(exportSession.error?.localizedDescription ?? "unknown")")
            return nil
        }
        
        return outputURL
    }
    
    // MARK: - Thumbnail Upload
    
    /// Upload thumbnail data to Supabase Storage.
    /// Returns the public URL string.
    func uploadThumbnail(data: Data, userUID: String, timelapseID: String) async throws -> String {
        let path = "\(userUID)/\(timelapseID).jpg"
        
        print("[SYNC] Uploading thumbnail: bucket=thumbnails, path=\(path), size=\(data.count / 1024)KB")
        
        do {
            try await supabase.storage.from("thumbnails").upload(
                path,
                data: data,
                options: FileOptions(contentType: "image/jpeg", upsert: true)
            )
        } catch {
            print("[SYNC] ❌ Thumbnail upload error: \(error)")
            throw error
        }
        
        let publicURL = try supabase.storage.from("thumbnails").getPublicURL(path: path)
        print("[SYNC] ✅ Thumbnail public URL: \(publicURL.absoluteString)")
        return publicURL.absoluteString
    }
    
    // MARK: - Profile Image Upload
    
    /// Upload a profile image to Supabase Storage, resized to 400x400.
    /// Returns the public URL string.
    func uploadProfileImage(data: Data, userUID: String) async throws -> String {
        guard let original = UIImage(data: data) else {
            throw StorageError.uploadFailed
        }
        
        // Resize to max 400x400
        let maxDimension: CGFloat = 400
        let size = original.size
        let scale = min(maxDimension / size.width, maxDimension / size.height, 1.0)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        
        let renderer = UIGraphicsImageRenderer(size: newSize)
        let resized = renderer.image { _ in
            original.draw(in: CGRect(origin: .zero, size: newSize))
        }
        
        guard let jpegData = resized.jpegData(compressionQuality: 0.8) else {
            throw StorageError.uploadFailed
        }
        
        let path = "\(userUID).jpg"
        
        try await supabase.storage.from("avatars").upload(
            path,
            data: jpegData,
            options: FileOptions(contentType: "image/jpeg", upsert: true)
        )
        
        let publicURL = try supabase.storage.from("avatars").getPublicURL(path: path)
        return publicURL.absoluteString
    }
    
    // MARK: - Wallpaper Upload
    
    /// Upload a group wallpaper image to Supabase Storage.
    func uploadWallpaper(data: Data, groupID: String) async throws -> String {
        let path = "groups/\(groupID).jpg"
        
        try await supabase.storage.from("wallpapers").upload(
            path,
            data: data,
            options: FileOptions(contentType: "image/jpeg", upsert: true)
        )
        
        let publicURL = try supabase.storage.from("wallpapers").getPublicURL(path: path)
        return publicURL.absoluteString
    }
    
    // MARK: - Delete Storage Files
    
    /// Delete video and thumbnail files from Supabase Storage for a timelapse.
    func deleteTimelapseFiles(userUID: String, timelapseID: String) async {
        let videoPath = "\(userUID)/\(timelapseID).mp4"
        let thumbPath = "\(userUID)/\(timelapseID).jpg"
        
        do {
            try await supabase.storage.from("timelapses").remove(paths: [videoPath])
        } catch {
            print("[STORAGE] Video delete failed for \(videoPath): \(error)")
        }
        
        do {
            try await supabase.storage.from("thumbnails").remove(paths: [thumbPath])
        } catch {
            print("[STORAGE] Thumbnail delete failed for \(thumbPath): \(error)")
        }
    }
    
    // MARK: - Errors
    
    enum StorageError: LocalizedError {
        case uploadFailed
        
        var errorDescription: String? {
            switch self {
            case .uploadFailed:
                return "Video upload failed."
            }
        }
    }
}
