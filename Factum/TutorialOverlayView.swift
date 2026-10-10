//
//  TutorialOverlayView.swift
//  Pigeon
//
//  Post-onboarding coach marks tutorial
//

import SwiftUI

// MARK: - Tutorial Steps (tab bar + profile)

enum TutorialStep: Int, CaseIterable {
    case feedTab = 0
    case friendsTab = 1
    case recordButton = 2
    // Steps 3-4 happen inside the camera (CameraTutorialStep)
    case groupsTab = 3
    case profileTab = 4
    case wallpaper = 5
    
    var title: String {
        switch self {
        case .feedTab:      return "Your Nest"
        case .friendsTab:   return "Your Flock"
        case .recordButton: return "Record"
        case .groupsTab:    return "Study Groups"
        case .profileTab:   return "Profile"
        case .wallpaper:    return "Make It Yours"
        }
    }
    
    var description: String {
        switch self {
        case .feedTab:      return "Study sessions show up here as posts. Share them or post to your Instagram Story."
        case .friendsTab:   return "Find friends, accept requests, and see what they're studying."
        case .recordButton: return "Start a timelapse, pomodoro, or timer session."
        case .groupsTab:    return "Create or join study groups with leaderboards."
        case .profileTab:   return "Your stats, streaks, subjects, and settings."
        case .wallpaper:    return "Customize your background with gradients, patterns, or your own photos. Find it in Settings."
        }
    }
    
    var icon: String {
        switch self {
        case .feedTab:      return "house.fill"
        case .friendsTab:   return "person.2.fill"
        case .recordButton: return "video.fill"
        case .groupsTab:    return "rectangle.3.group.bubble.fill"
        case .profileTab:   return "person.circle.fill"
        case .wallpaper:    return "photo.on.rectangle.angled"
        }
    }
    
    var buttonLabel: String {
        self == .wallpaper ? "Got it" : "Next"
    }
    
    var isLast: Bool {
        self == .wallpaper
    }
    
    /// Whether this step highlights a tab bar item with a spotlight cutout
    var hasTabSpotlight: Bool {
        switch self {
        case .feedTab, .friendsTab, .recordButton, .groupsTab, .profileTab: return true
        case .wallpaper: return false
        }
    }
    
    /// Whether the arrow points at a custom target rect (e.g. stats card) instead of tab bar
    var pointsAtCustomTarget: Bool {
        self == .wallpaper
    }
    
    /// Which tab index the spotlight / arrow points at (tab bar steps only)
    var arrowTabIndex: Int {
        switch self {
        case .feedTab:      return 0
        case .friendsTab:   return 1
        case .recordButton: return 2
        case .groupsTab:    return 3
        case .profileTab:   return 4
        case .wallpaper:    return 4
        }
    }
    
    /// Tab index to switch to when this step becomes active
    var targetTab: Int? {
        switch self {
        case .feedTab:      return 0
        case .friendsTab:   return 1
        case .recordButton: return nil
        case .groupsTab:    return 3
        case .profileTab:   return 4
        case .wallpaper:    return 4
        }
    }
    
    /// Whether advancing past this step opens the camera for in-camera tutorial
    var opensCamera: Bool {
        self == .recordButton
    }
}

// MARK: - Camera Tutorial Steps (shown inside TimelapseCameraView)

enum CameraTutorialStep: Int, CaseIterable {
    case subjects = 0
    case lockMode = 1
    
    var title: String {
        switch self {
        case .subjects: return "Study Subjects"
        case .lockMode: return "Lock Mode"
        }
    }
    
    var description: String {
        switch self {
        case .subjects: return "Pick a subject before you start recording."
        case .lockMode: return "Auto-saves when you leave the app and hides pause."
        }
    }
    
    var icon: String {
        switch self {
        case .subjects: return "book.fill"
        case .lockMode: return "lock.fill"
        }
    }
    
    var isLast: Bool { self == .lockMode }
}

/// Preference key for reporting the subject picker frame from TimelapseCameraView.
struct SubjectPickerFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        value = nextValue()
    }
}

// MARK: - Spotlight Overlay (dimmed background with cutout)

struct SpotlightOverlay: View {
    let targetRect: CGRect
    let cornerRadius: CGFloat = 14
    
    var body: some View {
        GeometryReader { geo in
            Path { path in
                path.addRect(CGRect(origin: .zero, size: geo.size))
                path.addRoundedRect(
                    in: targetRect.insetBy(dx: -6, dy: -6),
                    cornerSize: CGSize(width: cornerRadius, height: cornerRadius)
                )
            }
            .fill(style: FillStyle(eoFill: true))
            .foregroundStyle(Color.black.opacity(0.65))
        }
        .ignoresSafeArea()
    }
}

// MARK: - Arrow Shape

/// A small downward-pointing triangle used to connect the tooltip card to the spotlight.
struct TooltipArrow: Shape {
    func path(in rect: CGRect) -> Path {
        Path { p in
            p.move(to: CGPoint(x: rect.midX - 8, y: rect.minY))
            p.addLine(to: CGPoint(x: rect.midX + 8, y: rect.minY))
            p.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
            p.closeSubpath()
        }
    }
}

// MARK: - Tutorial Overlay View

/// Preference key for reporting the stats card frame from ProfileView.
struct StatsCardFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        value = nextValue()
    }
}

struct TutorialOverlayView: View {
    @Binding var isShowing: Bool
    @Binding var selectedTab: Int
    @Binding var showCameraTutorial: Bool
    /// The actual tab bar frame in global coordinates, passed from ContentView.
    let tabBarFrame: CGRect
    /// The "View Detailed Stats" card frame in global coordinates, passed from ProfileView.
    let statsCardFrame: CGRect
    @State private var currentStep: TutorialStep = .feedTab
    @State private var animateIn = false
    
    var body: some View {
        GeometryReader { geometry in
            let spotlight = spotlightRect(for: currentStep)
            
            ZStack {
                // Background overlay with spotlight cutout
                if spotlight != .zero {
                    SpotlightOverlay(targetRect: spotlight)
                        .transition(.opacity)
                } else {
                    Color.black.opacity(0.65)
                        .ignoresSafeArea()
                        .transition(.opacity)
                }
                
                // Tooltip card with arrow
                tooltipWithArrow(in: geometry, spotlightRect: spotlight)
                    .id(currentStep)
                    .transition(.opacity.combined(with: .offset(y: 8)))
            }
            .opacity(animateIn ? 1 : 0)
            .onAppear {
                withAnimation(.easeOut(duration: 0.4)) {
                    animateIn = true
                }
            }
            // When camera tutorial finishes, resume at groups step
            .onChange(of: showCameraTutorial) { _, isCameraShowing in
                if !isCameraShowing && currentStep == .recordButton {
                    selectedTab = 3
                    currentStep = .groupsTab
                    // Delay to let the full-screen cover dismiss
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        withAnimation(.easeOut(duration: 0.3)) {
                            animateIn = true
                        }
                    }
                }
            }
            // Block taps from reaching content below
            .contentShape(Rectangle())
            .onTapGesture { }
        }
        .ignoresSafeArea()
    }
    
    // MARK: - Spotlight Position Calculation
    
    /// Returns the center point of a tab icon in global coordinates.
    /// Matches the actual tab bar: HStack padding(.horizontal, 4), padding(.vertical, 4),
    /// each button has frame(height: 32) + padding(.vertical, 14) = 60pt tall.
    private func tabCenter(for tabIndex: Int) -> CGPoint {
        let containerHPad: CGFloat = 4
        let containerVPad: CGFloat = 4
        let buttonTotalHeight: CGFloat = 60  // 32pt icon frame + 14pt * 2 vertical padding
        
        let contentLeft = tabBarFrame.minX + containerHPad
        let contentWidth = tabBarFrame.width - containerHPad * 2
        let tabWidth = contentWidth / 5.0
        
        let cx = contentLeft + tabWidth * CGFloat(tabIndex) + tabWidth / 2
        let cy = tabBarFrame.minY + containerVPad + buttonTotalHeight / 2
        return CGPoint(x: cx, y: cy)
    }
    
    private func spotlightRect(for step: TutorialStep) -> CGRect {
        if step.hasTabSpotlight {
            let center = tabCenter(for: step.arrowTabIndex)
            let containerHPad: CGFloat = 4
            let contentWidth = tabBarFrame.width - containerHPad * 2
            let tabWidth = contentWidth / 5.0
            let spotWidth = tabWidth - 4
            let spotHeight: CGFloat = 60
            return CGRect(
                x: center.x - spotWidth / 2,
                y: center.y - spotHeight / 2,
                width: spotWidth,
                height: spotHeight
            )
        }
        return .zero
    }
    
    // MARK: - Tooltip with Arrow
    
    private func tooltipWithArrow(in geometry: GeometryProxy, spotlightRect: CGRect) -> some View {
        let screenWidth = geometry.size.width
        
        if currentStep.pointsAtCustomTarget {
            // Wallpaper step: centered card, no arrow, no spotlight
            return AnyView(
                VStack {
                    Spacer()
                    cardContent
                        .padding(.horizontal, 24)
                    Spacer()
                }
            )
        } else {
            // Tab bar steps: tooltip above the tab bar, arrow points DOWN
            let screenHeight = geometry.size.height
            let targetCenter = tabCenter(for: currentStep.arrowTabIndex)
            let arrowCenterX = targetCenter.x
            let anchorY: CGFloat = currentStep.hasTabSpotlight
                ? spotlightRect.minY - 12
                : tabBarFrame.minY - 12
            let bottomPad = max(0, screenHeight - anchorY)
            
            return AnyView(
                VStack(spacing: 0) {
                    Spacer()
                    
                    VStack(spacing: 0) {
                        cardContent
                        
                        // Arrow pointing down at the target tab
                        TooltipArrow()
                            .fill(PigeonTheme.cardBackground)
                            .frame(width: 16, height: 10)
                            .offset(x: arrowCenterX - screenWidth / 2)
                    }
                    .padding(.horizontal, 24)
                }
                .padding(.bottom, bottomPad)
            )
        }
    }
    
    // MARK: - Card Content
    
    private var cardContent: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Icon + title row
            HStack(spacing: 10) {
                Image(systemName: currentStep.icon)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(PigeonTheme.secondaryText)
                
                Text(currentStep.title)
                    .font(PigeonTheme.headlineFont)
                    .foregroundStyle(PigeonTheme.primaryText)
            }
            
            Text(currentStep.description)
                .font(PigeonTheme.font(16, weight: .light))
                .foregroundStyle(PigeonTheme.secondaryText)
                .fixedSize(horizontal: false, vertical: true)
            
            HStack {
                // Step indicator dots (8 total: 3 tab + 2 camera + 3 tab)
                HStack(spacing: 6) {
                    ForEach(0..<8, id: \.self) { i in
                        let isActive: Bool = {
                            switch i {
                            case 0: return currentStep == .feedTab
                            case 1: return currentStep == .friendsTab
                            case 2: return currentStep == .recordButton
                            // 3, 4 are camera steps — never active here
                            case 5: return currentStep == .groupsTab
                            case 6: return currentStep == .profileTab
                            case 7: return currentStep == .wallpaper
                            default: return false
                            }
                        }()
                        Circle()
                            .fill(isActive ? PigeonTheme.primaryText : PigeonTheme.separator)
                            .frame(width: 6, height: 6)
                    }
                }
                
                Spacer()
                
                // Skip button (except on last step)
                if !currentStep.isLast {
                    Button("Skip") {
                        Haptics.light()
                        dismissTutorial()
                    }
                    .font(PigeonTheme.captionFont)
                    .foregroundStyle(PigeonTheme.tertiaryText)
                    .padding(.trailing, 8)
                }
                
                // Next / Got it button
                Button(currentStep.buttonLabel) {
                    Haptics.light()
                    advanceStep()
                }
                .buttonStyle(PigeonButtonStyle())
            }
            .padding(.top, 2)
        }
        .padding(20)
        .background(PigeonTheme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.2), radius: 16, x: 0, y: 4)
    }
    
    // MARK: - Step Navigation
    
    private func advanceStep() {
        // After record step, open the camera for in-camera tutorial
        if currentStep.opensCamera {
            withAnimation(.easeOut(duration: 0.2)) {
                animateIn = false
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                showCameraTutorial = true
            }
            return
        }
        
        guard let next = TutorialStep(rawValue: currentStep.rawValue + 1) else {
            dismissTutorial()
            return
        }
        
        // Switch tab if needed
        if let tab = next.targetTab {
            selectedTab = tab
        }
        
        withAnimation(.easeInOut(duration: 0.35)) {
            currentStep = next
        }
    }
    
    private func dismissTutorial() {
        withAnimation(.easeOut(duration: 0.3)) {
            animateIn = false
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            isShowing = false
        }
    }
}
// MARK: - Camera Tutorial Overlay (shown inside TimelapseCameraView)

struct CameraTutorialOverlay: View {
    @Binding var isShowing: Bool
    let onFinished: () -> Void
    /// The subject picker card frame in global coordinates.
    let subjectPickerFrame: CGRect
    @State private var currentStep: CameraTutorialStep = .subjects
    @State private var animateIn = false
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Spotlight on the subject picker for step 1, dimmed for step 2
                if currentStep == .subjects && subjectPickerFrame != .zero {
                    SpotlightOverlay(targetRect: subjectPickerFrame)
                        .transition(.opacity)
                } else {
                    Color.black.opacity(0.65)
                        .ignoresSafeArea()
                        .transition(.opacity)
                }
                
                // Tooltip card
                cameraTooltip(in: geometry)
                    .id(currentStep)
                    .transition(.opacity.combined(with: .offset(y: 8)))
            }
            .opacity(animateIn ? 1 : 0)
            .onAppear {
                withAnimation(.easeOut(duration: 0.4)) {
                    animateIn = true
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { }
        }
        .ignoresSafeArea()
    }
    
    private func cameraTooltip(in geometry: GeometryProxy) -> some View {
        let screenWidth = geometry.size.width
        
        if currentStep == .subjects && subjectPickerFrame != .zero {
            // Position tooltip above the subject picker, arrow points down at it
            let screenHeight = geometry.size.height
            let arrowCenterX = subjectPickerFrame.midX
            let anchorY = subjectPickerFrame.minY - 12
            let bottomPad = max(0, screenHeight - anchorY)
            
            return AnyView(
                VStack(spacing: 0) {
                    Spacer()
                    VStack(spacing: 0) {
                        cameraCardContent
                        TooltipArrow()
                            .fill(PigeonTheme.cardBackground)
                            .frame(width: 16, height: 10)
                            .offset(x: arrowCenterX - screenWidth / 2)
                    }
                    .padding(.horizontal, 24)
                }
                .padding(.bottom, bottomPad)
            )
        } else {
            // Lock mode — centered card, no arrow
            return AnyView(
                VStack {
                    Spacer()
                    cameraCardContent
                        .padding(.horizontal, 24)
                    Spacer()
                }
            )
        }
    }
    
    private var cameraCardContent: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: currentStep.icon)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(PigeonTheme.secondaryText)
                
                Text(currentStep.title)
                    .font(PigeonTheme.headlineFont)
                    .foregroundStyle(PigeonTheme.primaryText)
            }
            
            Text(currentStep.description)
                .font(PigeonTheme.font(16, weight: .light))
                .foregroundStyle(PigeonTheme.secondaryText)
                .fixedSize(horizontal: false, vertical: true)
            
            HStack {
                // Step indicator dots (8 total, steps 3-4 are camera)
                HStack(spacing: 6) {
                    ForEach(0..<8, id: \.self) { i in
                        let isActive: Bool = {
                            switch i {
                            case 3: return currentStep == .subjects
                            case 4: return currentStep == .lockMode
                            default: return false
                            }
                        }()
                        Circle()
                            .fill(isActive ? PigeonTheme.primaryText : PigeonTheme.separator)
                            .frame(width: 6, height: 6)
                    }
                }
                
                Spacer()
                
                Button("Skip") {
                    Haptics.light()
                    dismissCameraTutorial()
                }
                .font(PigeonTheme.captionFont)
                .foregroundStyle(PigeonTheme.tertiaryText)
                .padding(.trailing, 8)
                
                Button("Next") {
                    Haptics.light()
                    advanceCameraStep()
                }
                .buttonStyle(PigeonButtonStyle())
            }
            .padding(.top, 2)
        }
        .padding(20)
        .background(PigeonTheme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.2), radius: 16, x: 0, y: 4)
    }
    
    private func advanceCameraStep() {
        if currentStep == .subjects {
            withAnimation(.easeInOut(duration: 0.35)) {
                currentStep = .lockMode
            }
        } else {
            dismissCameraTutorial()
        }
    }
    
    private func dismissCameraTutorial() {
        withAnimation(.easeOut(duration: 0.3)) {
            animateIn = false
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            isShowing = false
            onFinished()
        }
    }
}

