//
//  WallpaperPickerView.swift
//  Pigeon
//
//  Full wallpaper picker UI: gallery of presets, camera roll photos,
//  solid colors, and adjustment sliders. Supports home and group modes.
//

import SwiftUI
import PhotosUI

// MARK: - Mode

enum WallpaperPickerMode {
    case home
    case group(groupID: UUID, currentConfigJSON: String?)
}

// MARK: - Picker View

struct WallpaperPickerView: View {
    let mode: WallpaperPickerMode
    @Environment(WallpaperManager.self) private var wallpaperManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    
    @State private var config: WallpaperConfig = .defaultConfig
    @State private var selectedTab: WallpaperCategory = .gradients
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var localPhotoImage: UIImage? = nil
    @State private var customColor: Color = .gray
    @State private var isSaving = false
    @AppStorage("appearanceMode") private var appearanceMode: Int = 0
    
    var body: some View {
        VStack(spacing: 0) {
            // Live preview
            livePreview
                .frame(height: 240)
                .clipShape(RoundedRectangle(cornerRadius: PigeonTheme.cornerCard))
                .padding(.horizontal, 16)
                .padding(.top, 8)
            
            // Adjustment sliders
            adjustmentSliders
                .padding(.horizontal, 20)
                .padding(.top, 12)
            
            // Appearance mode
            HStack(spacing: 10) {
                Image(systemName: "circle.lefthalf.filled")
                    .font(.system(size: 14))
                    .foregroundStyle(PigeonTheme.secondaryText)
                Picker("Appearance", selection: $appearanceMode) {
                    Text("Auto").tag(0)
                    Text("Light").tag(1)
                    Text("Dark").tag(2)
                }
                .pickerStyle(.segmented)
                .onAppear {
                    let serifFont = UIFont(name: "Georgia", size: 13)
                        ?? UIFont.systemFont(ofSize: 13, weight: .light)
                    UISegmentedControl.appearance().setTitleTextAttributes(
                        [.font: serifFont], for: .normal
                    )
                    UISegmentedControl.appearance().setTitleTextAttributes(
                        [.font: serifFont], for: .selected
                    )
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 10)
            
            // Tab picker
            Picker("Category", selection: $selectedTab) {
                ForEach(WallpaperCategory.allCases, id: \.self) { cat in
                    Text(cat.rawValue).tag(cat)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 20)
            .padding(.top, 12)
            
            // Content grid
            ScrollView {
                switch selectedTab {
                case .gradients:
                    gradientGrid
                case .patterns:
                    patternGrid
                case .colors:
                    colorGrid
                case .photos:
                    photosTab
                }
            }
            .padding(.top, 8)
        }
        .background(PigeonTheme.background)
        .navigationTitle(toolbarTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                if isSaving {
                    ProgressView()
                        .tint(PigeonTheme.accent)
                } else {
                    Button("Save") { saveWallpaper() }
                        .font(PigeonTheme.bodyFont)
                        .foregroundStyle(PigeonTheme.accent)
                }
            }
        }
        .toolbarBackground(PigeonTheme.background, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .onAppear { loadCurrentConfig() }
        .onChange(of: selectedPhotoItem) { _, newItem in
            handlePhotoSelection(newItem)
        }
        .animation(.easeInOut(duration: 0.25), value: config.type)
        .animation(.easeInOut(duration: 0.25), value: config.presetID)
    }
    
    private var toolbarTitle: String {
        switch mode {
        case .home: return "Wallpaper"
        case .group: return "Group Wallpaper"
        }
    }
    
    // MARK: - Live Preview
    
    private var livePreview: some View {
        ZStack {
            WallpaperBackgroundView(config: config)
            
            // Mock feed content for preview
            VStack(spacing: 12) {
                // Mock greeting
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Good afternoon")
                            .font(PigeonTheme.captionFont)
                            .foregroundStyle(PigeonTheme.secondaryText)
                        Text("Preview")
                            .font(PigeonTheme.headlineFont)
                            .foregroundStyle(PigeonTheme.primaryText)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background {
                        if config.isCustom {
                            RoundedRectangle(cornerRadius: 10)
                                .fill(.ultraThinMaterial)
                        }
                    }
                    Spacer()
                }
                
                // Mock card
                HStack(spacing: 10) {
                    Circle()
                        .fill(Color.blue.opacity(0.3))
                        .frame(width: 32, height: 32)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Study Session")
                            .font(PigeonTheme.captionFont)
                            .foregroundStyle(PigeonTheme.primaryText)
                        Text("45m · Mathematics")
                            .font(PigeonTheme.smallFont)
                            .foregroundStyle(PigeonTheme.secondaryText)
                    }
                    Spacer()
                }
                .padding(12)
                .background {
                    if config.isCustom {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(.clear)
                            .glassEffect(in: .rect(cornerRadius: 12))
                    } else {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(PigeonTheme.cardBackground)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 12))
                
                Spacer()
            }
            .padding(16)
        }
    }
    
    // MARK: - Adjustment Sliders
    
    private var adjustmentSliders: some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: "aqi.medium")
                    .font(.system(size: 13))
                    .foregroundStyle(PigeonTheme.secondaryText)
                    .frame(width: 24)
                Text("Blur")
                    .font(PigeonTheme.captionFont)
                    .foregroundStyle(PigeonTheme.secondaryText)
                Slider(value: $config.blur, in: 0...20, step: 1)
                    .tint(PigeonTheme.accent)
                Text("\(Int(config.blur))")
                    .font(PigeonTheme.smallFont)
                    .foregroundStyle(PigeonTheme.tertiaryText)
                    .frame(width: 24)
            }
            
            HStack {
                Image(systemName: "circle.lefthalf.filled")
                    .font(.system(size: 13))
                    .foregroundStyle(PigeonTheme.secondaryText)
                    .frame(width: 24)
                Text("Dim")
                    .font(PigeonTheme.captionFont)
                    .foregroundStyle(PigeonTheme.secondaryText)
                Slider(value: $config.darkenOverlay, in: 0...0.5, step: 0.05)
                    .tint(PigeonTheme.accent)
                Text("\(Int(config.darkenOverlay * 100))%")
                    .font(PigeonTheme.smallFont)
                    .foregroundStyle(PigeonTheme.tertiaryText)
                    .frame(width: 32)
            }
            
            // Reset button
            HStack {
                Button {
                    Haptics.light()
                    withAnimation(.easeInOut(duration: 0.25)) {
                        config = .defaultConfig
                    }
                    localPhotoImage = nil
                    selectedPhotoItem = nil
                } label: {
                    Label("Reset", systemImage: "arrow.counterclockwise")
                        .font(PigeonTheme.smallFont)
                        .foregroundStyle(PigeonTheme.secondaryText)
                }
                Spacer()
            }
            .padding(.top, 4)
        }
    }
    
    // MARK: - Gradient Grid
    
    private var gradientGrid: some View {
        LazyVGrid(columns: [
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10)
        ], spacing: 10) {
            ForEach(BuiltInWallpapers.gradients) { preset in
                Button {
                    Haptics.selection()
                    config = WallpaperConfig(
                        type: .gradient,
                        presetID: preset.id,
                        blur: config.blur,
                        darkenOverlay: config.darkenOverlay
                    )
                    localPhotoImage = nil
                } label: {
                    VStack(spacing: 4) {
                        GradientWallpaperView(preset: preset)
                            .frame(height: 72)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay {
                                if config.presetID == preset.id && config.type == .gradient {
                                    RoundedRectangle(cornerRadius: 10)
                                        .strokeBorder(Color.white, lineWidth: 2.5)
                                }
                            }
                        Text(preset.name)
                            .font(PigeonTheme.smallFont)
                            .foregroundStyle(PigeonTheme.secondaryText)
                            .lineLimit(1)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 20)
    }
    
    // MARK: - Pattern Grid
    
    private var patternGrid: some View {
        LazyVGrid(columns: [
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10),
            GridItem(.flexible(), spacing: 10)
        ], spacing: 10) {
            ForEach(BuiltInWallpapers.patterns) { preset in
                Button {
                    Haptics.selection()
                    config = WallpaperConfig(
                        type: .pattern,
                        presetID: preset.id,
                        blur: config.blur,
                        darkenOverlay: config.darkenOverlay
                    )
                    localPhotoImage = nil
                } label: {
                    VStack(spacing: 4) {
                        PatternWallpaperView(presetID: preset.id)
                            .frame(height: 90)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay {
                                if config.presetID == preset.id && config.type == .pattern {
                                    RoundedRectangle(cornerRadius: 10)
                                        .strokeBorder(PigeonTheme.primaryText, lineWidth: 2.5)
                                }
                            }
                        Text(preset.name)
                            .font(PigeonTheme.smallFont)
                            .foregroundStyle(PigeonTheme.secondaryText)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 20)
    }
    
    // MARK: - Color Grid
    
    private var colorGrid: some View {
        VStack(spacing: 16) {
            // Preset colors
            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10)
            ], spacing: 10) {
                ForEach(BuiltInWallpapers.solidColors) { preset in
                    Button {
                        Haptics.selection()
                        config = WallpaperConfig(
                            type: .solidColor,
                            presetID: preset.id,
                            solidColorHex: preset.hex,
                            blur: config.blur,
                            darkenOverlay: config.darkenOverlay
                        )
                        localPhotoImage = nil
                    } label: {
                        VStack(spacing: 4) {
                            preset.color
                                .frame(height: 72)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .overlay {
                                    RoundedRectangle(cornerRadius: 10)
                                        .strokeBorder(
                                            config.presetID == preset.id && config.type == .solidColor
                                                ? Color.white : Color.white.opacity(0.15),
                                            lineWidth: config.presetID == preset.id && config.type == .solidColor ? 2.5 : 0.5
                                        )
                                }
                            Text(preset.name)
                                .font(PigeonTheme.smallFont)
                                .foregroundStyle(PigeonTheme.secondaryText)
                                .lineLimit(1)
                        }
                    }
                }
            }
            
            // Custom color picker
            HStack(spacing: 12) {
                ColorPicker("Custom Color", selection: $customColor, supportsOpacity: false)
                    .font(PigeonTheme.captionFont)
                    .foregroundStyle(PigeonTheme.primaryText)
                
                Button("Apply") {
                    Haptics.selection()
                    config = WallpaperConfig(
                        type: .solidColor,
                        presetID: nil,
                        solidColorHex: customColor.hexString,
                        blur: config.blur,
                        darkenOverlay: config.darkenOverlay
                    )
                    localPhotoImage = nil
                }
                .font(PigeonTheme.captionFont)
                .foregroundStyle(PigeonTheme.accent)
            }
            .padding(.horizontal, 4)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 20)
    }
    
    // MARK: - Photos Tab
    
    private var photosTab: some View {
        VStack(spacing: 20) {
            // Current photo preview (if a photo is selected)
            if config.type == .photo, let image = localPhotoImage {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 140)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay {
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(Color.white, lineWidth: 2.5)
                    }
                    .padding(.horizontal, 16)
            }
            
            // Pick photo button
            PhotosPicker(
                selection: $selectedPhotoItem,
                matching: .images,
                photoLibrary: .shared()
            ) {
                HStack(spacing: 10) {
                    Image(systemName: "photo.on.rectangle.angled")
                        .font(.system(size: 18))
                    Text(config.type == .photo ? "Choose Different Photo" : "Choose from Camera Roll")
                        .font(PigeonTheme.subheadlineFont)
                }
                .foregroundStyle(PigeonTheme.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(PigeonTheme.accent.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 16)
            
            if config.type != .photo {
                Text("Select a photo from your camera roll to use as your wallpaper")
                    .font(PigeonTheme.captionFont)
                    .foregroundStyle(PigeonTheme.tertiaryText)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 20)
    }
    
    // MARK: - Photo Selection
    
    private func handlePhotoSelection(_ item: PhotosPickerItem?) {
        guard let item else { return }
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self),
                  let image = UIImage(data: data) else { return }
            
            await MainActor.run {
                localPhotoImage = image
                
                // For home mode, save locally immediately for preview
                if case .home = mode {
                    if let savedURL = wallpaperManager.saveHomeWallpaperImage(image) {
                        config = WallpaperConfig(
                            type: .photo,
                            imageURL: savedURL,
                            blur: config.blur,
                            darkenOverlay: config.darkenOverlay
                        )
                    }
                } else {
                    // For group mode, we'll upload on save
                    // Use a temporary local preview
                    let tempDir = FileManager.default.temporaryDirectory
                    let tempURL = tempDir.appendingPathComponent("group_wallpaper_preview.jpg")
                    if let jpegData = image.jpegData(compressionQuality: 0.85) {
                        try? jpegData.write(to: tempURL)
                        config = WallpaperConfig(
                            type: .photo,
                            imageURL: tempURL.absoluteString,
                            blur: config.blur,
                            darkenOverlay: config.darkenOverlay
                        )
                    }
                }
            }
        }
    }
    
    // MARK: - Load / Save
    
    private func loadCurrentConfig() {
        switch mode {
        case .home:
            config = wallpaperManager.homeConfig
        case .group(_, let currentConfigJSON):
            config = wallpaperManager.groupConfig(from: currentConfigJSON)
        }
    }
    
    private func saveWallpaper() {
        isSaving = true
        
        switch mode {
        case .home:
            // If switching away from photo, clean up old file
            if config.type != .photo {
                wallpaperManager.removeHomeWallpaperImage()
            }
            wallpaperManager.homeConfig = config
            Haptics.success()
            dismiss()
            
        case .group(let groupID, _):
            Task {
                do {
                    var finalConfig = config
                    
                    // If photo, upload to Supabase
                    if config.type == .photo, let image = localPhotoImage {
                        if let resizedData = resizeForUpload(image) {
                            let publicURL = try await StorageService.shared.uploadWallpaper(
                                data: resizedData,
                                groupID: groupID.uuidString
                            )
                            finalConfig.imageURL = publicURL
                        }
                    }
                    
                    // Save to Supabase
                    if let json = finalConfig.jsonString {
                        try await SupabaseService.shared.updateGroupWallpaper(
                            groupID: groupID,
                            configJSON: json
                        )
                    }
                    
                    await MainActor.run {
                        Haptics.success()
                        isSaving = false
                        dismiss()
                    }
                } catch {
                    await MainActor.run {
                        isSaving = false
                        Haptics.error()
                        print("[WALLPAPER] Failed to save group wallpaper: \(error.localizedDescription)")
                    }
                }
            }
        }
    }
    
    private func resizeForUpload(_ image: UIImage) -> Data? {
        let maxDim: CGFloat = 1080
        let size = image.size
        let scale = min(maxDim / size.width, maxDim / size.height, 1.0)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        
        UIGraphicsBeginImageContextWithOptions(newSize, true, 1.0)
        image.draw(in: CGRect(origin: .zero, size: newSize))
        let resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return resized?.jpegData(compressionQuality: 0.8)
    }
}
