//
//  RecapsView.swift
//  Pigeon
//
//  Browsable weekly / monthly / yearly / all-time recap cards with sharing.
//

import SwiftUI

// MARK: - Period Enum

enum RecapPeriod: String, CaseIterable {
    case weekly  = "Week"
    case monthly = "Month"
    case yearly  = "Year"
    case allTime = "All Time"
}

// MARK: - Recaps View

struct RecapsView: View {
    @Environment(\.dismiss) private var dismiss
    let displayName: String
    let allTimelapses: [StudyTimelapse]
    
    @State private var selectedPeriod: RecapPeriod = .monthly
    @State private var referenceDate = Date()
    @State private var showShareSheet = false
    @State private var renderedImage: UIImage?
    @State private var showInstagramNotInstalled = false
    
    private let calendar = Calendar.current
    
    // MARK: Filtering
    
    private var filteredTimelapses: [StudyTimelapse] {
        switch selectedPeriod {
        case .weekly:
            guard let interval = calendar.dateInterval(of: .weekOfYear, for: referenceDate) else { return [] }
            return allTimelapses.filter { $0.createdAt >= interval.start && $0.createdAt < interval.end }
        case .monthly:
            guard let interval = calendar.dateInterval(of: .month, for: referenceDate) else { return [] }
            return allTimelapses.filter { $0.createdAt >= interval.start && $0.createdAt < interval.end }
        case .yearly:
            guard let interval = calendar.dateInterval(of: .year, for: referenceDate) else { return [] }
            return allTimelapses.filter { $0.createdAt >= interval.start && $0.createdAt < interval.end }
        case .allTime:
            return allTimelapses
        }
    }
    
    // MARK: Stats
    
    private var totalMinutes: Int {
        filteredTimelapses.reduce(0) { $0 + $1.durationSeconds } / 60
    }
    private var totalHours: Int { totalMinutes / 60 }
    private var remainderMins: Int { totalMinutes % 60 }
    private var sessionCount: Int { filteredTimelapses.count }
    
    private var longestSessionMinutes: Int {
        (filteredTimelapses.max(by: { $0.durationSeconds < $1.durationSeconds })?.durationSeconds ?? 0) / 60
    }
    
    private var topSubjects: [(name: String, minutes: Int)] {
        var dict: [String: Int] = [:]
        for tl in filteredTimelapses {
            for seg in tl.subjectSegments {
                dict[seg.subject, default: 0] += seg.seconds
            }
        }
        return dict.map { ($0.key, $0.value / 60) }
            .sorted { $0.1 > $1.1 }
            .prefix(3)
            .map { $0 }
    }
    
    private var totalAppLeaves: Int {
        filteredTimelapses.reduce(0) { $0 + $1.appLeaveCount }
    }
    
    private var focusScore: Int {
        max(0, 100 - totalAppLeaves * 2)
    }
    
    private var streakDays: Int {
        guard !allTimelapses.isEmpty else { return 0 }
        let studyDates = Set(allTimelapses.map { calendar.startOfDay(for: $0.createdAt) })
        let today = calendar.startOfDay(for: Date())
        var checkDate = today
        if !studyDates.contains(checkDate) {
            checkDate = calendar.date(byAdding: .day, value: -1, to: checkDate)!
            if !studyDates.contains(checkDate) { return 0 }
        }
        var streak = 0
        while studyDates.contains(checkDate) {
            streak += 1
            checkDate = calendar.date(byAdding: .day, value: -1, to: checkDate)!
        }
        return streak
    }
    
    // MARK: Date Navigation
    
    private var periodLabel: String {
        switch selectedPeriod {
        case .weekly:
            guard let interval = calendar.dateInterval(of: .weekOfYear, for: referenceDate) else { return "" }
            return "Week of \(interval.start.formatted(.dateTime.month(.abbreviated).day()))"
        case .monthly:
            return referenceDate.formatted(.dateTime.month(.wide).year())
        case .yearly:
            return referenceDate.formatted(.dateTime.year())
        case .allTime:
            return "All Time"
        }
    }
    
    private var cardSubtitle: String {
        switch selectedPeriod {
        case .weekly:  return "pigeon. weekly recap"
        case .monthly: return "pigeon. monthly recap"
        case .yearly:  return "pigeon. yearly recap"
        case .allTime: return "pigeon. wrapped"
        }
    }
    
    private var canGoForward: Bool {
        switch selectedPeriod {
        case .weekly:
            guard let interval = calendar.dateInterval(of: .weekOfYear, for: referenceDate),
                  let current = calendar.dateInterval(of: .weekOfYear, for: Date()) else { return false }
            return interval.start < current.start
        case .monthly:
            guard let interval = calendar.dateInterval(of: .month, for: referenceDate),
                  let current = calendar.dateInterval(of: .month, for: Date()) else { return false }
            return interval.start < current.start
        case .yearly:
            guard let interval = calendar.dateInterval(of: .year, for: referenceDate),
                  let current = calendar.dateInterval(of: .year, for: Date()) else { return false }
            return interval.start < current.start
        case .allTime:
            return false
        }
    }
    
    private func navigateBack() {
        withAnimation(.easeInOut(duration: 0.2)) {
            switch selectedPeriod {
            case .weekly:  referenceDate = calendar.date(byAdding: .weekOfYear, value: -1, to: referenceDate) ?? referenceDate
            case .monthly: referenceDate = calendar.date(byAdding: .month, value: -1, to: referenceDate) ?? referenceDate
            case .yearly:  referenceDate = calendar.date(byAdding: .year, value: -1, to: referenceDate) ?? referenceDate
            case .allTime: break
            }
        }
    }
    
    private func navigateForward() {
        guard canGoForward else { return }
        withAnimation(.easeInOut(duration: 0.2)) {
            switch selectedPeriod {
            case .weekly:  referenceDate = calendar.date(byAdding: .weekOfYear, value: 1, to: referenceDate) ?? referenceDate
            case .monthly: referenceDate = calendar.date(byAdding: .month, value: 1, to: referenceDate) ?? referenceDate
            case .yearly:  referenceDate = calendar.date(byAdding: .year, value: 1, to: referenceDate) ?? referenceDate
            case .allTime: break
            }
        }
    }
    
    // MARK: Body
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Period picker
                    PigeonUnderlinePicker(
                        tabs: RecapPeriod.allCases,
                        selection: $selectedPeriod,
                        label: { $0.rawValue }
                    )
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    
                    // Date navigator
                    if selectedPeriod != .allTime {
                        HStack(spacing: 16) {
                            Button { navigateBack() } label: {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(PigeonTheme.primaryText)
                                    .frame(width: 32, height: 32)
                            }
                            
                            Text(periodLabel)
                                .font(.gloucester(size: 16))
                                .foregroundStyle(PigeonTheme.primaryText)
                                .frame(maxWidth: .infinity)
                            
                            Button { navigateForward() } label: {
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(canGoForward ? PigeonTheme.primaryText : PigeonTheme.tertiaryText)
                                    .frame(width: 32, height: 32)
                            }
                            .disabled(!canGoForward)
                        }
                        .padding(.horizontal, 24)
                    }
                    
                    // The card
                    recapCard
                        .padding(.top, 4)
                    
                    // Share buttons
                    VStack(spacing: 12) {
                        Button {
                            Haptics.medium()
                            renderCard()
                            guard InstagramStoriesSharer.isInstagramInstalled else {
                                showInstagramNotInstalled = true
                                return
                            }
                            shareToInstagram()
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 16))
                                Text("Share to Instagram Stories")
                                    .font(PigeonTheme.subheadlineFont)
                            }
                            .foregroundStyle(PigeonTheme.background)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(PigeonTheme.primaryText)
                            .clipShape(OrganicRect(base: 12))
                        }
                        
                        Button {
                            Haptics.light()
                            renderCard()
                            showShareSheet = true
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "square.and.arrow.up")
                                    .font(.system(size: 16))
                                Text("Save or Share")
                                    .font(PigeonTheme.subheadlineFont)
                            }
                            .foregroundStyle(PigeonTheme.primaryText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(PigeonTheme.cardBackground)
                            .clipShape(OrganicRect(base: 12))
                            .overlay(
                                OrganicRect(base: 12)
                                    .strokeBorder(PigeonTheme.separator, lineWidth: 1)
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.bottom, 40)
            }
            .background(PigeonTheme.background)
            .toolbar(.hidden, for: .navigationBar)
            .overlay(alignment: .topTrailing) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(PigeonTheme.secondaryText)
                        .frame(width: 32, height: 32)
                        .background(PigeonTheme.cardBackground)
                        .clipShape(Circle())
                }
                .padding(.trailing, 20)
                .padding(.top, 12)
            }
            .sheet(isPresented: $showShareSheet) {
                if let image = renderedImage {
                    ShareSheet(items: [image])
                }
            }
            .alert("Instagram Not Installed", isPresented: $showInstagramNotInstalled) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Install Instagram to share your stats as a story.")
            }
        }
        .presentationBackground(PigeonTheme.background)
    }
    
    // MARK: - Accent color per period
    
    private var accentColor: Color {
        switch selectedPeriod {
        case .weekly:  return Color(red: 0.35, green: 0.65, blue: 0.55)  // sage green
        case .monthly: return Color(red: 0.72, green: 0.52, blue: 0.35)  // warm amber
        case .yearly:  return Color(red: 0.55, green: 0.42, blue: 0.65)  // soft purple
        case .allTime: return Color(red: 0.78, green: 0.62, blue: 0.40)  // gold
        }
    }
    
    private var gradientTop: Color {
        switch selectedPeriod {
        case .weekly:  return Color(red: 0.06, green: 0.12, blue: 0.10)
        case .monthly: return Color(red: 0.14, green: 0.09, blue: 0.05)
        case .yearly:  return Color(red: 0.10, green: 0.07, blue: 0.14)
        case .allTime: return Color(red: 0.12, green: 0.10, blue: 0.05)
        }
    }
    
    private var gradientBottom: Color {
        Color(red: 0.06, green: 0.05, blue: 0.05)
    }
    
    // MARK: - Recap Card (Preview)
    
    private var recapCard: some View {
        recapCardContent(
            primary: .white,
            secondary: .white.opacity(0.5),
            tertiary: .white.opacity(0.35),
            accent: accentColor,
            bgTop: gradientTop,
            bgBottom: gradientBottom
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.3), radius: 30, y: 12)
        .padding(.horizontal, 20)
    }
    
    @ViewBuilder
    private func recapCardContent(
        primary: Color, secondary: Color, tertiary: Color,
        accent: Color, bgTop: Color, bgBottom: Color
    ) -> some View {
        ZStack {
            // Gradient background
            LinearGradient(
                colors: [bgTop, bgBottom],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            
            // Decorative accent glow
            Circle()
                .fill(accent.opacity(0.15))
                .frame(width: 300, height: 300)
                .blur(radius: 80)
                .offset(x: 80, y: -60)
            
            Circle()
                .fill(accent.opacity(0.08))
                .frame(width: 200, height: 200)
                .blur(radius: 60)
                .offset(x: -100, y: 200)
            
            // Content
            VStack(spacing: 0) {
                // Header
                HStack {
                    PigeonIcon(size: 24, color: accent.opacity(0.7))
                    Spacer()
                    Text(cardSubtitle)
                        .font(.gloucester(size: 11))
                        .foregroundStyle(secondary)
                        .textCase(.uppercase)
                        .tracking(3)
                }
                .padding(.horizontal, 24)
                .padding(.top, 28)
                
                Spacer().frame(height: 32)
                
                // Hero stat
                VStack(spacing: 6) {
                    if selectedPeriod != .allTime {
                        Text(periodLabel)
                            .font(.gloucester(size: 14))
                            .foregroundStyle(accent)
                    }
                    
                    Text("\(totalHours)")
                        .font(.gloucester(size: 96))
                        .foregroundStyle(primary)
                        .shadow(color: accent.opacity(0.3), radius: 20, y: 0)
                    
                    Text("hours studied")
                        .font(.gloucester(size: 18))
                        .foregroundStyle(secondary)
                    
                    if remainderMins > 0 {
                        Text("and \(remainderMins) minutes")
                            .font(.gloucester(size: 13))
                            .foregroundStyle(tertiary)
                            .padding(.top, 2)
                    }
                }
                
                Spacer().frame(height: 32)
                
                if filteredTimelapses.isEmpty {
                    Text("no sessions this period")
                        .font(.gloucester(size: 15))
                        .foregroundStyle(secondary)
                        .padding(.vertical, 24)
                } else {
                    // Stat pills row
                    HStack(spacing: 8) {
                        statPill(value: "\(sessionCount)", label: "sessions", primary: primary, tertiary: tertiary, accent: accent)
                        if selectedPeriod == .allTime {
                            statPill(value: "\(streakDays)", label: "streak", primary: primary, tertiary: tertiary, accent: accent)
                        } else {
                            statPill(value: "\(focusScore)%", label: "focus", primary: primary, tertiary: tertiary, accent: accent)
                        }
                        statPill(value: "\(longestSessionMinutes)m", label: "longest", primary: primary, tertiary: tertiary, accent: accent)
                    }
                    .padding(.horizontal, 20)
                    
                    Spacer().frame(height: 24)
                    
                    // Subject bars
                    if !topSubjects.isEmpty {
                        let maxMins = topSubjects.first?.minutes ?? 1
                        
                        VStack(alignment: .leading, spacing: 10) {
                            Text("TOP SUBJECTS")
                                .font(.gloucester(size: 10))
                                .foregroundStyle(tertiary)
                                .tracking(2)
                            
                            ForEach(Array(topSubjects.enumerated()), id: \.offset) { index, subject in
                                HStack(spacing: 10) {
                                    Text("\(index + 1)")
                                        .font(.gloucester(size: 14))
                                        .foregroundStyle(tertiary)
                                        .frame(width: 16)
                                    
                                    GeometryReader { geo in
                                        let fraction = maxMins > 0 ? CGFloat(subject.minutes) / CGFloat(maxMins) : 0
                                        RoundedRectangle(cornerRadius: 6)
                                            .fill(accent.opacity(Double(1.0) - Double(index) * 0.25))
                                            .frame(width: max(geo.size.width * fraction, 40))
                                            .overlay(alignment: .leading) {
                                                Text(subject.name)
                                                    .font(.gloucester(size: 13))
                                                    .foregroundStyle(primary)
                                                    .lineLimit(1)
                                                    .padding(.leading, 10)
                                            }
                                    }
                                    .frame(height: 30)
                                    
                                    Text("\(subject.minutes / 60)h \(subject.minutes % 60)m")
                                        .font(.gloucester(size: 12))
                                        .foregroundStyle(tertiary)
                                        .frame(width: 52, alignment: .trailing)
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                }
                
                Spacer().frame(height: 20)
                
                // Thin accent line
                Rectangle()
                    .fill(accent.opacity(0.2))
                    .frame(height: 1)
                    .padding(.horizontal, 24)
                
                // Footer
                HStack {
                    Text(displayName.lowercased())
                        .font(.gloucester(size: 12))
                        .foregroundStyle(tertiary)
                    Spacer()
                    Text("pigeon")
                        .font(.gloucester(size: 12))
                        .foregroundStyle(tertiary)
                        .tracking(1)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
        }
    }
    
    private func statPill(value: String, label: String, primary: Color, tertiary: Color, accent: Color) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.gloucester(size: 20))
                .foregroundStyle(primary)
            Text(label)
                .font(.gloucester(size: 10))
                .foregroundStyle(tertiary)
                .textCase(.uppercase)
                .tracking(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(accent.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(accent.opacity(0.15), lineWidth: 1)
        )
    }
    
    // MARK: - Export Card (hardcoded colors for ImageRenderer)
    
    private var exportAccent: Color {
        switch selectedPeriod {
        case .weekly:  return Color(red: 0.35, green: 0.65, blue: 0.55)
        case .monthly: return Color(red: 0.72, green: 0.52, blue: 0.35)
        case .yearly:  return Color(red: 0.55, green: 0.42, blue: 0.65)
        case .allTime: return Color(red: 0.78, green: 0.62, blue: 0.40)
        }
    }
    
    private var exportGradientTop: Color {
        switch selectedPeriod {
        case .weekly:  return Color(red: 0.06, green: 0.12, blue: 0.10)
        case .monthly: return Color(red: 0.14, green: 0.09, blue: 0.05)
        case .yearly:  return Color(red: 0.10, green: 0.07, blue: 0.14)
        case .allTime: return Color(red: 0.12, green: 0.10, blue: 0.05)
        }
    }
    
    private var recapCardForExport: some View {
        recapCardContent(
            primary: .white,
            secondary: .white.opacity(0.5),
            tertiary: .white.opacity(0.35),
            accent: exportAccent,
            bgTop: exportGradientTop,
            bgBottom: Color(red: 0.06, green: 0.05, blue: 0.05)
        )
        .frame(width: 360)
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
    
    // MARK: - Rendering & Sharing
    
    @MainActor
    private func renderCard() {
        let renderer = ImageRenderer(content: recapCardForExport)
        renderer.scale = 3
        renderedImage = renderer.uiImage
    }
    
    @MainActor
    private func shareToInstagram() {
        guard let image = renderedImage,
              let pngData = image.pngData() else { return }
        
        let bundleID = Bundle.main.bundleIdentifier ?? ""
        guard let url = URL(string: "instagram-stories://share?source_application=\(bundleID)") else { return }
        guard UIApplication.shared.canOpenURL(url) else { return }
        
        let pasteboardItems: [[String: Any]] = [
            ["com.instagram.sharedSticker.stickerImage": pngData]
        ]
        let options: [UIPasteboard.OptionsKey: Any] = [
            .expirationDate: Date().addingTimeInterval(60 * 5)
        ]
        UIPasteboard.general.setItems(pasteboardItems, options: options)
        UIApplication.shared.open(url)
    }
}
