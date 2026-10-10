//
//  BuiltInWallpapers.swift
//  Pigeon
//
//  Catalog of built-in gradient, pattern, and solid color wallpaper presets.
//  All rendered programmatically — no asset files needed.
//

import SwiftUI

// MARK: - Wallpaper Category

enum WallpaperCategory: String, CaseIterable, Sendable {
    case gradients = "Gradients"
    case patterns = "Patterns"
    case colors = "Colors"
    case photos = "Photos"
}

// MARK: - Built-In Wallpapers

enum BuiltInWallpapers {
    
    // MARK: Gradient Presets
    
    struct GradientPreset: Identifiable, Sendable {
        let id: String
        let name: String
        let colors: [Color]
        let startPoint: UnitPoint
        let endPoint: UnitPoint
    }
    
    static let gradients: [GradientPreset] = [
        GradientPreset(
            id: "gradient_sunset",
            name: "Sunset",
            colors: [Color(red: 0.96, green: 0.42, blue: 0.26), Color(red: 0.85, green: 0.24, blue: 0.45), Color(red: 0.55, green: 0.18, blue: 0.56)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_ocean",
            name: "Ocean",
            colors: [Color(red: 0.05, green: 0.20, blue: 0.45), Color(red: 0.08, green: 0.45, blue: 0.55), Color(red: 0.10, green: 0.58, blue: 0.52)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_forest",
            name: "Forest",
            colors: [Color(red: 0.10, green: 0.25, blue: 0.12), Color(red: 0.15, green: 0.42, blue: 0.18), Color(red: 0.22, green: 0.55, blue: 0.25)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_lavender",
            name: "Lavender",
            colors: [Color(red: 0.55, green: 0.35, blue: 0.75), Color(red: 0.75, green: 0.50, blue: 0.80), Color(red: 0.90, green: 0.70, blue: 0.85)],
            startPoint: .topLeading, endPoint: .bottomTrailing
        ),
        GradientPreset(
            id: "gradient_midnight",
            name: "Midnight",
            colors: [Color(red: 0.05, green: 0.05, blue: 0.15), Color(red: 0.10, green: 0.10, blue: 0.25), Color(red: 0.15, green: 0.08, blue: 0.20)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_ember",
            name: "Ember",
            colors: [Color(red: 0.35, green: 0.08, blue: 0.05), Color(red: 0.65, green: 0.18, blue: 0.08), Color(red: 0.85, green: 0.35, blue: 0.10)],
            startPoint: .bottomLeading, endPoint: .topTrailing
        ),
        GradientPreset(
            id: "gradient_arctic",
            name: "Arctic",
            colors: [Color(red: 0.78, green: 0.88, blue: 0.95), Color(red: 0.55, green: 0.75, blue: 0.90), Color(red: 0.40, green: 0.60, blue: 0.85)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_golden",
            name: "Golden Hour",
            colors: [Color(red: 0.95, green: 0.75, blue: 0.30), Color(red: 0.90, green: 0.55, blue: 0.25), Color(red: 0.80, green: 0.40, blue: 0.20)],
            startPoint: .topTrailing, endPoint: .bottomLeading
        ),
        GradientPreset(
            id: "gradient_moss",
            name: "Moss",
            colors: [Color(red: 0.35, green: 0.38, blue: 0.22), Color(red: 0.48, green: 0.50, blue: 0.30), Color(red: 0.60, green: 0.58, blue: 0.40)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_storm",
            name: "Storm",
            colors: [Color(red: 0.15, green: 0.15, blue: 0.18), Color(red: 0.25, green: 0.25, blue: 0.30), Color(red: 0.35, green: 0.35, blue: 0.38)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_blush",
            name: "Blush",
            colors: [Color(red: 0.95, green: 0.75, blue: 0.75), Color(red: 0.92, green: 0.60, blue: 0.65), Color(red: 0.88, green: 0.50, blue: 0.58)],
            startPoint: .topLeading, endPoint: .bottomTrailing
        ),
        GradientPreset(
            id: "gradient_dusk",
            name: "Dusk",
            colors: [Color(red: 0.15, green: 0.08, blue: 0.30), Color(red: 0.25, green: 0.12, blue: 0.45), Color(red: 0.10, green: 0.10, blue: 0.30)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_parchment",
            name: "Parchment+",
            colors: [Color(red: 0.96, green: 0.90, blue: 0.82), Color(red: 0.92, green: 0.85, blue: 0.76), Color(red: 0.88, green: 0.80, blue: 0.70)],
            startPoint: .top, endPoint: .bottom
        ),
        GradientPreset(
            id: "gradient_noir",
            name: "Noir",
            colors: [Color(red: 0.0, green: 0.0, blue: 0.0), Color(red: 0.08, green: 0.08, blue: 0.08), Color(red: 0.0, green: 0.0, blue: 0.0)],
            startPoint: .topLeading, endPoint: .bottomTrailing
        ),
    ]
    
    // MARK: Pattern Presets
    
    struct PatternPreset: Identifiable, Sendable {
        let id: String
        let name: String
    }
    
    static let patterns: [PatternPreset] = [
        PatternPreset(id: "pattern_dots", name: "Dots"),
        PatternPreset(id: "pattern_lines", name: "Lines"),
        PatternPreset(id: "pattern_grid", name: "Grid"),
        PatternPreset(id: "pattern_waves", name: "Waves"),
        PatternPreset(id: "pattern_feathers", name: "Feathers"),
    ]
    
    // MARK: Solid Color Presets
    
    struct ColorPreset: Identifiable, Sendable {
        let id: String
        let name: String
        let hex: String
        var color: Color { Color(hex: hex) ?? .gray }
    }
    
    static let solidColors: [ColorPreset] = [
        ColorPreset(id: "color_black", name: "Black", hex: "0A0A0A"),
        ColorPreset(id: "color_white", name: "White", hex: "F5F5F0"),
        ColorPreset(id: "color_warmgray", name: "Warm Gray", hex: "8C857A"),
        ColorPreset(id: "color_navy", name: "Navy", hex: "1B2838"),
        ColorPreset(id: "color_forest", name: "Forest", hex: "1A3A2A"),
        ColorPreset(id: "color_burgundy", name: "Burgundy", hex: "4A1525"),
        ColorPreset(id: "color_slate", name: "Slate", hex: "3A3F47"),
        ColorPreset(id: "color_cream", name: "Cream", hex: "F0E6D4"),
    ]
    
    // MARK: Lookup
    
    static func gradient(for id: String) -> GradientPreset? {
        gradients.first { $0.id == id }
    }
    
    static func pattern(for id: String) -> PatternPreset? {
        patterns.first { $0.id == id }
    }
    
    static func solidColor(for id: String) -> ColorPreset? {
        solidColors.first { $0.id == id }
    }
}

// MARK: - Gradient View

struct GradientWallpaperView: View {
    let preset: BuiltInWallpapers.GradientPreset
    
    var body: some View {
        LinearGradient(
            colors: preset.colors,
            startPoint: preset.startPoint,
            endPoint: preset.endPoint
        )
    }
}

// MARK: - Pattern Views

struct PatternWallpaperView: View {
    let presetID: String
    @Environment(\.colorScheme) private var colorScheme
    
    private var patternColor: Color {
        colorScheme == .dark
            ? Color.white.opacity(0.06)
            : Color.black.opacity(0.06)
    }
    
    var body: some View {
        ZStack {
            PigeonTheme.background
            
            Canvas { context, size in
                switch presetID {
                case "pattern_dots":
                    drawDots(context: context, size: size)
                case "pattern_lines":
                    drawLines(context: context, size: size)
                case "pattern_grid":
                    drawGrid(context: context, size: size)
                case "pattern_waves":
                    drawWaves(context: context, size: size)
                case "pattern_feathers":
                    drawFeathers(context: context, size: size)
                default:
                    break
                }
            }
        }
    }
    
    private func drawDots(context: GraphicsContext, size: CGSize) {
        let spacing: CGFloat = 24
        let radius: CGFloat = 2.5
        let color = UIColor(patternColor)
        
        var y: CGFloat = spacing / 2
        while y < size.height {
            var x: CGFloat = spacing / 2
            while x < size.width {
                let rect = CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)
                context.fill(Path(ellipseIn: rect), with: .color(Color(color)))
                x += spacing
            }
            y += spacing
        }
    }
    
    private func drawLines(context: GraphicsContext, size: CGSize) {
        let spacing: CGFloat = 18
        let color = UIColor(patternColor)
        let totalDistance = size.width + size.height
        var offset: CGFloat = -size.height
        
        while offset < totalDistance {
            var path = Path()
            path.move(to: CGPoint(x: offset, y: 0))
            path.addLine(to: CGPoint(x: offset + size.height, y: size.height))
            context.stroke(path, with: .color(Color(color)), lineWidth: 1)
            offset += spacing
        }
    }
    
    private func drawGrid(context: GraphicsContext, size: CGSize) {
        let spacing: CGFloat = 32
        let color = UIColor(patternColor)
        
        // Vertical lines
        var x: CGFloat = 0
        while x < size.width {
            var path = Path()
            path.move(to: CGPoint(x: x, y: 0))
            path.addLine(to: CGPoint(x: x, y: size.height))
            context.stroke(path, with: .color(Color(color)), lineWidth: 0.5)
            x += spacing
        }
        
        // Horizontal lines
        var y: CGFloat = 0
        while y < size.height {
            var path = Path()
            path.move(to: CGPoint(x: 0, y: y))
            path.addLine(to: CGPoint(x: size.width, y: y))
            context.stroke(path, with: .color(Color(color)), lineWidth: 0.5)
            y += spacing
        }
    }
    
    private func drawWaves(context: GraphicsContext, size: CGSize) {
        let waveHeight: CGFloat = 8
        let waveLength: CGFloat = 60
        let spacing: CGFloat = 28
        let color = UIColor(patternColor)
        
        var y: CGFloat = spacing
        while y < size.height {
            var path = Path()
            path.move(to: CGPoint(x: 0, y: y))
            var x: CGFloat = 0
            while x < size.width {
                let controlX = x + waveLength / 4
                let endX = x + waveLength / 2
                let controlY = y + (Int(x / waveLength) % 2 == 0 ? -waveHeight : waveHeight)
                path.addQuadCurve(to: CGPoint(x: endX, y: y), control: CGPoint(x: controlX, y: controlY))
                x = endX
            }
            context.stroke(path, with: .color(Color(color)), lineWidth: 1)
            y += spacing
        }
    }
    
    private func drawFeathers(context: GraphicsContext, size: CGSize) {
        let spacing: CGFloat = 48
        let featherSize: CGFloat = 12
        let color = UIColor(patternColor)
        
        var row = 0
        var y: CGFloat = spacing / 2
        while y < size.height {
            var x: CGFloat = (row % 2 == 0) ? spacing / 2 : spacing
            while x < size.width {
                // Simple feather shape: ellipse + stem
                let ellipseRect = CGRect(x: x - featherSize / 3, y: y - featherSize / 2, width: featherSize / 1.5, height: featherSize)
                context.fill(Path(ellipseIn: ellipseRect), with: .color(Color(color)))
                
                // Stem
                var stemPath = Path()
                stemPath.move(to: CGPoint(x: x, y: y - featherSize / 2))
                stemPath.addLine(to: CGPoint(x: x, y: y + featherSize / 2 + 3))
                context.stroke(stemPath, with: .color(Color(color)), lineWidth: 0.8)
                
                x += spacing
            }
            y += spacing
            row += 1
        }
    }
}

// MARK: - Pattern Thumbnail (for picker grid)

struct PatternThumbnailView: View {
    let presetID: String
    
    var body: some View {
        PatternWallpaperView(presetID: presetID)
            .frame(width: 100, height: 100)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
