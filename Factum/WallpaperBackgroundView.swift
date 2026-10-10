//
//  WallpaperBackgroundView.swift
//  Pigeon
//
//  Renders any WallpaperConfig as a full-screen background layer.
//  Handles gradients, patterns, solid colors, and photos (local + remote).
//

import SwiftUI

struct WallpaperBackgroundView: View {
    let config: WallpaperConfig
    
    var body: some View {
        ZStack {
            // Fallback base
            PigeonTheme.background
            
            // Wallpaper content
            wallpaperContent
            
            // Blur overlay
            if config.blur > 0 {
                Rectangle()
                    .fill(.ultraThinMaterial)
                    .opacity(min(config.blur / 20.0, 1.0))
            }
            
            // Darken overlay for readability
            if config.darkenOverlay > 0 {
                Color.black.opacity(config.darkenOverlay)
            }
        }
        .ignoresSafeArea()
    }
    
    @ViewBuilder
    private var wallpaperContent: some View {
        switch config.type {
        case .default:
            EmptyView()
            
        case .gradient:
            if let presetID = config.presetID,
               let preset = BuiltInWallpapers.gradient(for: presetID) {
                GradientWallpaperView(preset: preset)
            }
            
        case .pattern:
            if let presetID = config.presetID {
                PatternWallpaperView(presetID: presetID)
            }
            
        case .solidColor:
            if let hex = config.solidColorHex, let color = Color(hex: hex) {
                color
            } else if let presetID = config.presetID,
                      let preset = BuiltInWallpapers.solidColor(for: presetID) {
                preset.color
            }
            
        case .photo:
            if let imageURL = config.imageURL {
                photoView(for: imageURL)
            }
        }
    }
    
    @ViewBuilder
    private func photoView(for urlString: String) -> some View {
        if urlString.hasPrefix("http"),
           let url = URL(string: urlString) {
            // Remote URL (group wallpaper from Supabase)
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    PigeonTheme.background
                case .empty:
                    PigeonTheme.background
                        .overlay {
                            ProgressView()
                                .tint(PigeonTheme.secondaryText)
                        }
                @unknown default:
                    PigeonTheme.background
                }
            }
        } else if let fileURL = WallpaperManager.resolvedImageURL(for: urlString),
                  let uiImage = UIImage(contentsOfFile: fileURL.path) {
            // Local file — resolved via WallpaperManager (handles both
            // legacy absolute paths and new relative paths)
            Image(uiImage: uiImage)
                .resizable()
                .aspectRatio(contentMode: .fill)
        }
    }
}

// MARK: - Wallpaper Thumbnail (for picker grid)

struct WallpaperThumbnailView: View {
    let config: WallpaperConfig
    let size: CGFloat
    
    init(config: WallpaperConfig, size: CGFloat = 80) {
        self.config = config
        self.size = size
    }
    
    var body: some View {
        WallpaperBackgroundView(config: config)
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(Color.white.opacity(0.1), lineWidth: 0.5)
            )
    }
}
