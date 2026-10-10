//
//  WallpaperManager.swift
//  Pigeon
//
//  Observable manager for home and group wallpaper state.
//  Injected into the environment from PigeonApp.
//

import SwiftUI
import Observation

/// Cached wallpaper directory to avoid repeated FileManager lookups.
private let wallpaperDirectory: URL = {
    let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    let dir = docs.appendingPathComponent("wallpapers", isDirectory: true)
    try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
    return dir
}()

@Observable
final class WallpaperManager {
    
    /// The user's home screen wallpaper configuration.
    var homeConfig: WallpaperConfig {
        didSet { saveHomeConfig() }
    }
    
    var hasCustomHomeWallpaper: Bool {
        homeConfig.type != .default
    }
    
    init() {
        if let json = UserDefaults.standard.string(forKey: "homeWallpaper"),
           let config = WallpaperConfig.from(json: json) {
            self.homeConfig = config
        } else {
            self.homeConfig = .defaultConfig
        }
    }
    
    private func saveHomeConfig() {
        if homeConfig.type == .default {
            UserDefaults.standard.removeObject(forKey: "homeWallpaper")
        } else {
            UserDefaults.standard.set(homeConfig.jsonString, forKey: "homeWallpaper")
        }
    }
    
    /// Parse a group's wallpaper config from its stored JSON string.
    func groupConfig(from json: String?) -> WallpaperConfig {
        guard let json, let config = WallpaperConfig.from(json: json) else {
            return .defaultConfig
        }
        return config
    }
    
    // MARK: - Home Wallpaper Image Storage
    
    /// Save a camera roll image to local Documents and return a relative path string.
    /// We store a relative path ("wallpapers/home_wallpaper.jpg") instead of an absolute
    /// file URL so the wallpaper survives app updates that change the container UUID.
    func saveHomeWallpaperImage(_ image: UIImage) -> String? {
        guard let data = image.jpegData(compressionQuality: 0.85) else { return nil }
        let filePath = wallpaperDirectory.appendingPathComponent("home_wallpaper.jpg")
        do {
            try data.write(to: filePath, options: .atomic)
            return "wallpapers/home_wallpaper.jpg"
        } catch {
            print("[WALLPAPER] Failed to save home wallpaper: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Resolves a wallpaper imageURL to an absolute file URL.
    /// Handles both legacy absolute "file://" paths and new relative paths.
    static func resolvedImageURL(for imageURL: String) -> URL? {
        if imageURL.hasPrefix("file://") {
            // Legacy absolute path — try it first, then fall back to relative resolution
            if let url = URL(string: imageURL), FileManager.default.fileExists(atPath: url.path) {
                return url
            }
            // Legacy path is stale (container UUID changed) — try extracting relative portion
            if let range = imageURL.range(of: "wallpapers/") {
                let relative = String(imageURL[range.lowerBound...])
                let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let resolved = docs.appendingPathComponent(relative)
                if FileManager.default.fileExists(atPath: resolved.path) {
                    return resolved
                }
            }
            return nil
        } else if imageURL.hasPrefix("http") {
            return URL(string: imageURL)
        } else {
            // Relative path — resolve against Documents directory
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let resolved = docs.appendingPathComponent(imageURL)
            if FileManager.default.fileExists(atPath: resolved.path) {
                return resolved
            }
            return nil
        }
    }
    
    /// Remove the locally saved home wallpaper image.
    func removeHomeWallpaperImage() {
        let filePath = wallpaperDirectory.appendingPathComponent("home_wallpaper.jpg")
        try? FileManager.default.removeItem(at: filePath)
    }
}
