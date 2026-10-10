//
//  PigeonFlightMapView.swift
//  Pigeon
//
//  Live map showing a pigeon flying between landmarks during Carrier Pigeon study sessions.
//  Camera rides on the pigeon's back — zoomed in tight, pitched forward, terrain scrolling beneath.
//

import SwiftUI
import MapKit

// MARK: - Coordinate Helper

extension CityLandmark {
    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

// MARK: - PigeonFlightMapView

struct PigeonFlightMapView: View {
    let flight: PigeonFlight
    let progress: Double
    
    enum Style {
        case camera
        case themed
    }
    var style: Style = .themed
    
    /// Tracks previous progress value + timestamp so we can lerp smoothly.
    @State private var previousProgress: Double = 0
    @State private var currentProgress: Double = 0
    @State private var lastUpdateTime: Date = .now
    @State private var cameraPosition: MapCameraPosition = .automatic
    
    private var mapHeight: CGFloat {
        style == .camera ? 150 : 220
    }
    
    /// Heading from origin to destination in degrees (0 = north, 90 = east)
    private var flightHeading: Double {
        let oLat = flight.origin.latitude * .pi / 180
        let dLat = flight.destination.latitude * .pi / 180
        let dLng = (flight.destination.longitude - flight.origin.longitude) * .pi / 180
        let x = cos(dLat) * sin(dLng)
        let y = cos(oLat) * sin(dLat) - sin(oLat) * cos(dLat) * cos(dLng)
        let bearing = atan2(x, y) * 180 / .pi
        return (bearing + 360).truncatingRemainder(dividingBy: 360)
    }
    
    private func coordinateAt(_ t: Double) -> CLLocationCoordinate2D {
        let clamped = min(max(t, 0), 1)
        let lat = flight.origin.latitude + (flight.destination.latitude - flight.origin.latitude) * clamped
        let lng = flight.origin.longitude + (flight.destination.longitude - flight.origin.longitude) * clamped
        return CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }
    
    var body: some View {
        // TimelineView drives smooth camera interpolation between 1-second progress ticks
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let now = timeline.date
            let elapsed = now.timeIntervalSince(lastUpdateTime)
            // Lerp from previousProgress toward currentProgress over 1 second
            let lerpT = min(elapsed / 1.0, 1.0)
            let smoothed = previousProgress + (currentProgress - previousProgress) * lerpT
            let coord = coordinateAt(smoothed)
            
            Map(position: $cameraPosition, interactionModes: []) {
                MapPolyline(coordinates: [
                    flight.origin.coordinate,
                    flight.destination.coordinate
                ])
                .stroke(routeLineColor, style: StrokeStyle(lineWidth: 2, dash: [8, 4]))
                
                Annotation("", coordinate: flight.destination.coordinate) {
                    landmarkDot
                }
            }
            .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
            .onChange(of: coord.latitude) { _, _ in
                cameraPosition = .camera(MapCamera(
                    centerCoordinate: coord,
                    distance: 500,
                    heading: flightHeading,
                    pitch: 70
                ))
            }
        }
        .frame(height: mapHeight)
        .overlay {
            PigeonPOVOverlay(style: style)
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius)
                .strokeBorder(borderColor, lineWidth: 1)
        )
        .allowsHitTesting(false)
        .onAppear {
            previousProgress = progress
            currentProgress = progress
            lastUpdateTime = .now
            let coord = coordinateAt(progress)
            cameraPosition = .camera(MapCamera(
                centerCoordinate: coord,
                distance: 500,
                heading: flightHeading,
                pitch: 70
            ))
        }
        .onChange(of: progress) { oldVal, newVal in
            previousProgress = oldVal
            currentProgress = newVal
            lastUpdateTime = .now
        }
    }
    
    // MARK: - Style Properties
    
    private var landmarkDot: some View {
        Circle()
            .fill(dotColor)
            .frame(width: 8, height: 8)
            .overlay(Circle().strokeBorder(dotBorderColor, lineWidth: 1.5))
    }
    
    private var routeLineColor: Color {
        style == .camera ? .white.opacity(0.6) : PigeonTheme.accent
    }
    
    private var dotColor: Color {
        style == .camera ? .white : PigeonTheme.primaryText
    }
    
    private var dotBorderColor: Color {
        style == .camera ? .white.opacity(0.5) : PigeonTheme.accent
    }
    
    private var borderColor: Color {
        style == .camera ? .white.opacity(0.15) : PigeonTheme.separator
    }
    
    private var cornerRadius: CGFloat {
        style == .camera ? 10 : 14
    }
}

// MARK: - First-Person Pigeon POV Overlay
//
// Flap-glide pattern: pigeons in cruising flight mostly glide with wings
// extended, punctuated by short bursts of 3-4 fast flaps. This cycle repeats.

struct PigeonPOVOverlay: View {
    var style: PigeonFlightMapView.Style = .themed
    
    // -- Flap-glide timing --
    // Full macro-cycle: ~4s glide → ~1.2s flapping (3 fast flaps at ~0.4s each)
    private let glideDuration: Double = 4.0
    private let flapBurstDuration: Double = 1.2  // 3 flaps × 0.4s each
    private let singleFlapDuration: Double = 0.4
    private let downstrokeFraction: Double = 0.53
    
    private var macroCycleDuration: Double { glideDuration + flapBurstDuration }
    
    private var silhouetteColor: Color {
        style == .camera ? Color(white: 0.06) : Color(white: 0.04)
    }
    
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 60.0)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            let phase = flapState(at: t)
            
            GeometryReader { geo in
                let w = geo.size.width
                let h = geo.size.height
                
                let bodyX = w * 0.5
                let wingW = w * 0.52
                let wingH = h * 0.65
                let wingCenterY = h * 0.82
                
                // --- Left wing ---
                PigeonWingShape(side: .left, flap: phase.angle, fold: phase.fold)
                    .fill(silhouetteColor)
                    .frame(width: wingW, height: wingH)
                    .position(x: bodyX - wingW / 2, y: wingCenterY)
                
                // --- Right wing ---
                PigeonWingShape(side: .right, flap: phase.angle, fold: phase.fold)
                    .fill(silhouetteColor)
                    .frame(width: wingW, height: wingH)
                    .position(x: bodyX + wingW / 2, y: wingCenterY)
                
                // --- Head and neck (overlaps wing roots) ---
                PigeonHeadShape()
                    .fill(silhouetteColor)
                    .frame(width: w * 0.34, height: h * 0.52)
                    .position(x: bodyX, y: h * 0.84)
            }
        }
        .allowsHitTesting(false)
    }
    
    // MARK: - Flap-Glide Physics
    
    private struct FlapState {
        var angle: Double  // -1 (bottom) to +1 (top), 0 = neutral/glide
        var fold: Double   // 0 (extended) to ~0.3 (folded on upstroke)
    }
    
    private func flapState(at time: Double) -> FlapState {
        let macroTime = time.truncatingRemainder(dividingBy: macroCycleDuration)
        
        if macroTime < glideDuration {
            // --- GLIDING: wings extended, still, level ---
            return FlapState(angle: 0, fold: 0)
        } else {
            // --- FLAP BURST: 3 fast flaps ---
            let burstTime = macroTime - glideDuration
            let flapTime = burstTime.truncatingRemainder(dividingBy: singleFlapDuration)
            let downDuration = singleFlapDuration * downstrokeFraction
            let upDuration = singleFlapDuration * (1 - downstrokeFraction)
            
            if flapTime < downDuration {
                // Downstroke: 0 → -1
                let t = flapTime / downDuration
                let eased = sin(t * .pi / 2)
                return FlapState(angle: -eased, fold: 0)
            } else {
                // Upstroke: -1 → 0
                let t = (flapTime - downDuration) / upDuration
                let eased = sin(t * .pi / 2)
                let foldCurve = sin(t * .pi)
                return FlapState(angle: -(1.0 - eased), fold: foldCurve * 0.25)
            }
        }
    }
}

// MARK: - Wing Shape
//
// Hinged wing: the root stays FIXED. The wrist moves a little. The tip moves A LOT.
// This creates the characteristic bend at the wrist during flapping.
//
// From dorsal (first-person) view, downstroke foreshortens as the wing sweeps below
// the body plane — so the tip's Y increases (moves toward the body/bottom of frame)
// and the chord compresses slightly. The tip also lags behind, shifting backward (X).
//
// `flap`: -1 = full downstroke, 0 = level/glide, approaches +1 = upstroke
// `fold`: 0 = extended, ~0.25 = folded on upstroke

struct PigeonWingShape: Shape {
    enum Side { case left, right }
    var side: Side
    var flap: Double = 0    // -1 (down) to 0 (level)
    var fold: Double = 0    // 0 (extended) to ~0.25 (folded)
    
    var animatableData: AnimatablePair<Double, Double> {
        get { AnimatablePair(flap, fold) }
        set { flap = newValue.first; fold = newValue.second }
    }
    
    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let h = rect.height
        let fl = CGFloat(min(max(flap, -1), 1))   // flap: -1...+1
        let fo = CGFloat(min(max(fold, 0), 1))     // fold: 0...1
        
        // --- Hinge offsets ---
        // Each point gets a Y offset proportional to its distance from root.
        // Root (right edge, x=w) = 0 offset. Tip (left edge, x=0) = max offset.
        // Downstroke (fl < 0) → tip moves DOWN (positive Y in screen coords).
        // The offset function: for a point at normalized distance `d` from root (0...1),
        // offset = -fl * d * d * maxDisplacement  (quadratic for whip effect)
        let maxTipY = h * 0.30      // tip can sweep ±30% of height
        let maxWristY = h * 0.06    // wrist barely moves (it's close to body)
        
        // Wrist is ~60% out from root → d=0.6
        let wristYOff = -fl * 0.36 * maxTipY   // 0.6^2 = 0.36
        // Tip is 100% out → d=1.0
        let tipYOff = -fl * maxTipY             // 1.0^2 = 1.0
        // Mid-outer (between wrist and tip) → d=0.8
        let midOuterYOff = -fl * 0.64 * maxTipY // 0.8^2 = 0.64
        // Inner wing points (d=0.2, 0.4) move very little
        let innerYOff1 = -fl * 0.04 * maxTipY  // 0.2^2
        let innerYOff2 = -fl * 0.16 * maxTipY  // 0.4^2
        
        // Tip also sweeps backward (toward body) during downstroke due to lag
        let tipXLag = -fl * w * 0.06   // tip shifts right (toward root) on downstroke
        
        // Fold (upstroke) shifts outer wing inward
        let foldX = fo * w * 0.14
        let foldY = fo * h * 0.08
        
        // --- Key anatomy points ---
        let rootX = w
        let rootY = h * 0.45
        
        let wristX = w * 0.38
        let wristY = h * 0.22 + wristYOff
        
        let tipX = w * 0.0 + foldX + tipXLag
        let tipY = h * 0.12 + foldY + tipYOff
        
        var p = Path()
        
        // === LEADING EDGE ===
        
        p.move(to: CGPoint(x: rootX, y: rootY))
        
        // Root → wrist (inner wing — barely moves)
        p.addCurve(
            to: CGPoint(x: wristX, y: wristY),
            control1: CGPoint(x: w * 0.78, y: h * 0.28 + innerYOff2),
            control2: CGPoint(x: w * 0.55, y: wristY + h * 0.02)
        )
        
        // Wrist → tip (outer wing — moves dramatically)
        p.addCurve(
            to: CGPoint(x: tipX, y: tipY),
            control1: CGPoint(x: w * 0.24 + fo * w * 0.06, y: h * 0.14 + midOuterYOff + fo * h * 0.03),
            control2: CGPoint(x: w * 0.10 + fo * w * 0.08 + tipXLag * 0.5, y: tipY + h * 0.01)
        )
        
        // === WINGTIP ===
        p.addQuadCurve(
            to: CGPoint(x: tipX + w * 0.04, y: tipY + h * 0.10),
            control: CGPoint(x: tipX - w * 0.01, y: tipY + h * 0.05)
        )
        
        // === TRAILING EDGE — PRIMARIES (tip → wrist) ===
        
        // Feather 1
        p.addQuadCurve(
            to: CGPoint(x: tipX + w * 0.12, y: tipY + h * 0.20),
            control: CGPoint(x: tipX + w * 0.06, y: tipY + h * 0.18)
        )
        p.addQuadCurve(
            to: CGPoint(x: tipX + w * 0.14, y: tipY + h * 0.18),
            control: CGPoint(x: tipX + w * 0.14, y: tipY + h * 0.21)
        )
        
        // Feather 2 (mid-outer wing)
        let f2x = w * 0.24 + fo * w * 0.05 + tipXLag * 0.6
        let f2y = h * 0.38 + midOuterYOff + fo * h * 0.04
        p.addQuadCurve(
            to: CGPoint(x: f2x, y: f2y),
            control: CGPoint(x: w * 0.18 + fo * w * 0.05 + tipXLag * 0.7, y: h * 0.32 + midOuterYOff * 0.8 + fo * h * 0.04)
        )
        p.addQuadCurve(
            to: CGPoint(x: f2x + w * 0.03, y: f2y - h * 0.02),
            control: CGPoint(x: f2x + w * 0.02, y: f2y + h * 0.03)
        )
        
        // Feather 3 (at wrist)
        p.addQuadCurve(
            to: CGPoint(x: wristX + w * 0.04, y: wristY + h * 0.22),
            control: CGPoint(x: w * 0.32, y: h * 0.44 + wristYOff * 0.5)
        )
        p.addQuadCurve(
            to: CGPoint(x: wristX + w * 0.08, y: wristY + h * 0.20),
            control: CGPoint(x: wristX + w * 0.04, y: wristY + h * 0.24)
        )
        
        // === TRAILING EDGE — SECONDARIES (wrist → root, barely move) ===
        
        // Feather 4
        p.addQuadCurve(
            to: CGPoint(x: w * 0.56, y: h * 0.60 + innerYOff2),
            control: CGPoint(x: w * 0.48, y: h * 0.54 + innerYOff2)
        )
        p.addQuadCurve(
            to: CGPoint(x: w * 0.60, y: h * 0.58 + innerYOff2),
            control: CGPoint(x: w * 0.58, y: h * 0.62 + innerYOff2)
        )
        
        // Feather 5
        p.addQuadCurve(
            to: CGPoint(x: w * 0.70, y: h * 0.70 + innerYOff1),
            control: CGPoint(x: w * 0.64, y: h * 0.66 + innerYOff1)
        )
        p.addQuadCurve(
            to: CGPoint(x: w * 0.74, y: h * 0.68 + innerYOff1),
            control: CGPoint(x: w * 0.72, y: h * 0.72 + innerYOff1)
        )
        
        // Feather 6
        p.addQuadCurve(
            to: CGPoint(x: w * 0.84, y: h * 0.78),
            control: CGPoint(x: w * 0.78, y: h * 0.74)
        )
        p.addQuadCurve(
            to: CGPoint(x: w * 0.88, y: h * 0.76),
            control: CGPoint(x: w * 0.86, y: h * 0.80)
        )
        
        // Tertials (at body — zero offset)
        p.addQuadCurve(
            to: CGPoint(x: w * 0.96, y: h * 0.58),
            control: CGPoint(x: w * 0.94, y: h * 0.68)
        )
        
        // Close back to root
        p.addQuadCurve(
            to: CGPoint(x: rootX, y: rootY),
            control: CGPoint(x: w * 0.99, y: h * 0.50)
        )
        
        p.closeSubpath()
        
        if side == .right {
            let mirror = CGAffineTransform(scaleX: -1, y: 1)
                .concatenating(CGAffineTransform(translationX: w, y: 0))
            return p.applying(mirror)
        }
        return p
    }
}

// MARK: - Head Shape

struct PigeonHeadShape: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let h = rect.height
        
        var p = Path()
        
        // Neck base
        p.move(to: CGPoint(x: w * 0.15, y: h))
        
        // Left side of neck
        p.addCurve(
            to: CGPoint(x: w * 0.30, y: h * 0.45),
            control1: CGPoint(x: w * 0.12, y: h * 0.78),
            control2: CGPoint(x: w * 0.22, y: h * 0.55)
        )
        
        // Neck → head
        p.addCurve(
            to: CGPoint(x: w * 0.38, y: h * 0.25),
            control1: CGPoint(x: w * 0.32, y: h * 0.38),
            control2: CGPoint(x: w * 0.34, y: h * 0.30)
        )
        
        // Left side of head
        p.addQuadCurve(
            to: CGPoint(x: w * 0.45, y: h * 0.14),
            control: CGPoint(x: w * 0.36, y: h * 0.16)
        )
        
        // Crown
        p.addQuadCurve(
            to: CGPoint(x: w * 0.5, y: h * 0.11),
            control: CGPoint(x: w * 0.47, y: h * 0.10)
        )
        
        // Beak
        p.addLine(to: CGPoint(x: w * 0.495, y: h * 0.04))
        p.addLine(to: CGPoint(x: w * 0.505, y: h * 0.04))
        p.addLine(to: CGPoint(x: w * 0.5, y: h * 0.11))
        
        // Right side of crown
        p.addQuadCurve(
            to: CGPoint(x: w * 0.55, y: h * 0.14),
            control: CGPoint(x: w * 0.53, y: h * 0.10)
        )
        
        // Right side of head
        p.addQuadCurve(
            to: CGPoint(x: w * 0.62, y: h * 0.25),
            control: CGPoint(x: w * 0.64, y: h * 0.16)
        )
        
        // Right side of neck
        p.addCurve(
            to: CGPoint(x: w * 0.70, y: h * 0.45),
            control1: CGPoint(x: w * 0.66, y: h * 0.30),
            control2: CGPoint(x: w * 0.68, y: h * 0.38)
        )
        
        p.addCurve(
            to: CGPoint(x: w * 0.85, y: h),
            control1: CGPoint(x: w * 0.78, y: h * 0.55),
            control2: CGPoint(x: w * 0.88, y: h * 0.78)
        )
        
        p.closeSubpath()
        return p
    }
}

// MARK: - Map Cache Pre-warming

enum MapCacheService {
    @MainActor
    static func prewarmCache(for flight: PigeonFlight) {
        let oLat = flight.origin.latitude
        let oLng = flight.origin.longitude
        let dLat = flight.destination.latitude
        let dLng = flight.destination.longitude
        
        let options = MKMapSnapshotter.Options()
        options.region = MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: (oLat + dLat) / 2,
                longitude: (oLng + dLng) / 2
            ),
            span: MKCoordinateSpan(
                latitudeDelta: max(abs(oLat - dLat) * 1.5, 0.02),
                longitudeDelta: max(abs(oLng - dLng) * 1.5, 0.02)
            )
        )
        options.size = CGSize(width: 400, height: 300)
        options.scale = UIScreen.main.scale
        
        let snapshotter = MKMapSnapshotter(options: options)
        snapshotter.start { snapshot, error in
            if let error {
                print("[MAP] Cache pre-warm failed: \(error.localizedDescription)")
            }
        }
    }
}
