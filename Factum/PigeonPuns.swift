import SwiftUI

// MARK: - Pun Rarity

enum PunRarity: String, CaseIterable, Codable {
    case common
    case uncommon
    case rare
    case epic
    case legendary
    case mythic
    
    var displayName: String {
        rawValue
    }
    
    var color: Color {
        switch self {
        case .common:    return Color(red: 0.60, green: 0.58, blue: 0.54)
        case .uncommon:  return Color(red: 0.40, green: 0.72, blue: 0.44)
        case .rare:      return Color(red: 0.30, green: 0.70, blue: 0.78)
        case .epic:      return Color(red: 0.62, green: 0.40, blue: 0.82)
        case .legendary: return Color(red: 0.85, green: 0.72, blue: 0.30)
        case .mythic:    return Color(red: 0.88, green: 0.28, blue: 0.28)
        }
    }
    
    /// Feed boost in seconds — rarer posts appear as if posted this many seconds in the future.
    var feedBoostSeconds: TimeInterval {
        switch self {
        case .common:    return 0
        case .uncommon:  return 30 * 60        // 30 min
        case .rare:      return 60 * 60        // 1 hour
        case .epic:      return 2 * 60 * 60    // 2 hours
        case .legendary: return 4 * 60 * 60    // 4 hours
        case .mythic:    return 8 * 60 * 60    // 8 hours
        }
    }
    
    /// Cumulative probability weight out of 100.
    private var cumulativeWeight: Double {
        switch self {
        case .common:    return 60
        case .uncommon:  return 85    // 60 + 25
        case .rare:      return 95    // 85 + 10
        case .epic:      return 98.5  // 95 + 3.5
        case .legendary: return 99.5  // 98.5 + 1
        case .mythic:    return 100   // 99.5 + 0.5
        }
    }
    
    /// Roll a random rarity tier using weighted probabilities.
    static func roll() -> PunRarity {
        let value = Double.random(in: 0..<100)
        for tier in PunRarity.allCases {
            if value < tier.cumulativeWeight {
                return tier
            }
        }
        return .common
    }
}

// MARK: - Pun Library

struct PigeonPunLibrary {
    struct TaggedPun {
        let text: String
        let rarity: PunRarity
    }
    
    static let allPuns: [TaggedPun] = commonPuns + uncommonPuns + rarePuns + epicPuns + legendaryPuns + mythicPuns
    
    // 180 common puns
    private static let commonPuns: [TaggedPun] = [
        "just winging it",
        "pecking away at the books",
        "strutting through my notes",
        "roosting at my desk as usual",
        "bobbing my head through these flashcards",
        "just a pigeon doing pigeon things",
        "no crumbs left behind in these notes",
        "flocking to the library again",
        "nesting into a good study groove",
        "perched at my desk",
        "cooing over my progress",
        "bread crumbs can wait",
        "peck peck peck",
        "sitting on these notes like eggs",
        "another day another coo",
        "head bobbing through homework",
        "same perch different day",
        "this pigeon showed up",
        "doing pigeon things",
        "back at the nest",
        "desk pigeon reporting for duty",
        "the grind never stops cooing",
        "pigeon at work do not disturb",
        "feathers down books open",
        "coo coo clock says it's study time",
        "fluffed up and ready to learn",
        "waddle waddle study study",
        "found a good crumb of knowledge",
        "pigeon brain in overdrive",
        "sitting on the ledge of productivity",
        "just a bird with a textbook",
        "pecking order: study first",
        "another day on the wire",
        "feathered and focused",
        "walking circles around this problem",
        "head bob approved",
        "park bench scholar",
        "crumb chaser turned page turner",
        "one pigeon one mission",
        "the coop is quiet tonight",
        "bread later books now",
        "pigeon mode activated",
        "waddling through the syllabus",
        "just vibing on my perch",
        "landed on the right page",
        "this ledge has good wifi",
        "coo-ld be worse",
        "on my perch grinding",
        "flew to the library didnt look back",
        "the flock is asleep but im up",
        "pigeon with a plan",
        "tail feathers tucked in lets go",
        "another brick in the nest",
        "found my study spot",
        "cooing quietly to myself",
        "notebook open beak shut",
        "one page at a time one coo at a time",
        "rooftop scholar reporting in",
        "the early worm can wait im studying",
        "pigeon punching in for the night shift",
        "still here still cooing",
        "desk crumbs are the only crumbs i need",
        "pigeon clocked in",
        "study perch occupied",
        "flap flap focus",
        "the nest can wait",
        "wings folded brain unfolded",
        "pecking at this textbook like corn",
        "pigeon hours are study hours",
        "just doing the bird thing",
        "coo-zy study session",
        "sitting pretty on these notes",
        "the ledge life chose me",
        "beak deep in chapter three",
        "pigeon in the zone",
        "one more page then breadcrumbs",
        "perched and productive",
        "the wire is my office",
        "bobbing along to these lectures",
        "pigeon brain loading",
        "just a regular study bird",
        "cooing at my notes hoping they coo back",
        "feathers ruffled but still going",
        "another sunset another study session",
        "my nest my rules my notes",
        "pigeon on a mission cant stop wont stop",
        "the rooftop is my campus",
        "study bird does study things",
        "pecking away one problem at a time",
        "found a nice windowsill to study on",
        "bread is temporary grades are forever",
        "pigeon reporting for academic duty",
        "the coop called i said im busy",
        "head down feathers up",
        "just a pigeon with ambitions",
        "waddled all the way to the library for this",
        "cooing at my laptop screen",
        "same bird different textbook",
        "another chapter another coo",
        "pigeon studying in the wild",
        "my feathers say chill but my brain says grind",
        "parked on this desk like a fire hydrant pigeon",
        "the breadline can wait",
        "study o'clock in the coop",
        "pigeon brains assemble",
        "just pecked through the introduction",
        "bobbing to the rhythm of turning pages",
        "this pigeon has places to be and notes to read",
        "coo-rrently studying",
        "waddling my way through the problem set",
        "the statue i sit on would be proud",
        "i came i cooed i studied",
        "pigeon perspective on this chapter: confusing but trying",
        "just flapped in from the last session",
        "study crumbs are the best crumbs",
        "perching through the pain",
        "the flock can wait i have homework",
        "low key cooing through this assignment",
        "pigeon brain go brrr",
        "just a city bird trying to get educated",
        "sitting on this problem like an egg",
        "the grind is real the pigeon is realer",
        "notes? pecked. coffee? cooed. lets go",
        "my nest has a study corner now",
        "another page another head bob",
        "pigeon at the desk again surprise surprise",
        "cooing through the confusion",
        "bread can wait knowledge cant",
        "winged scholar at your service",
        "beak to book ratio is good tonight",
        "just a pigeon and its flashcards",
        "the park can wait",
        "pigeon brain buffering",
        "reading these notes like theyre breadcrumbs on the sidewalk",
        "study bird checking in",
        "coo-ntinuing where i left off",
        "this pigeon doesnt quit",
        "feathered student in session",
        "sitting on the problem until it hatches",
        "pigeon productivity at an all time medium",
        "the wire is electric but so is my focus",
        "just bobbing through another assignment",
        "pigeon life chose books tonight",
        "one wing typing one wing flipping pages",
        "cooed my way through the first section",
        "back at it like a pigeon to breadcrumbs",
        "my perch my pace my notes",
        "still studying still pigeoning",
        "the rooftop library is open",
        "pecking through page after page",
        "pigeon zen mode activated",
        "flew in sat down opened the book",
        "head bob count: too many to track",
        "just a feathered friend and a stack of notes",
        "cooing softly at my progress",
        "the nest is warm but the grind is warmer",
        "waddled to my desk and here we are",
        "pigeon with a pencil watch out",
        "study session number who even knows anymore",
        "the crumbs of knowledge are everywhere",
        "beak first into these notes",
        "pigeon clocking overtime tonight",
        "just pecking and hoping for the best",
        "another study session another coo",
        "rooftop grind dont stop",
        "pigeon brain engaged",
        "cooing at this textbook like its a friend",
        "perched up and locked in",
        "pigeon status: at desk",
        "somewhere between bread and brilliance",
        "the pigeons are studying and so am i",
        "winged and ready to learn something",
        "one coo closer to understanding",
        "pigeon pacing around these notes",
        "the ledge review session continues",
        "just fluffed up and cracked open the book",
        "pigeon scholars anonymous meeting in session",
        "the fountain outside is calling but so are these notes",
        "traded my breadcrumbs for bookmarks",
        "this pigeon runs on focus and fountain water",
    ].map { TaggedPun(text: $0, rarity: .common) }
    
    // 60 uncommon puns
    private static let uncommonPuns: [TaggedPun] = [
        "coo coo for this topic",
        "feeling fly after that session",
        "spreading my wings one chapter at a time",
        "migrating through the material",
        "preening my notes into perfection",
        "coasting on tail feathers and caffeine",
        "delivering results one page at a time",
        "plucking out the key concepts",
        "feathering my nest with knowledge",
        "took flight with this chapter",
        "too cool to coo about it",
        "ruffled some feathers with this one",
        "hatching new ideas over here",
        "flew right through that chapter",
        "the flock would be proud",
        "wings up for this one",
        "found the seed of understanding",
        "soaring through the review sheet",
        "making my nest a little smarter",
        "cleared for takeoff on chapter five",
        "left no crumb unturned",
        "feather by feather building knowledge",
        "upgraded from park pigeon to library pigeon",
        "the migration to understanding has begun",
        "cooing my way to the dean's list",
        "pigeon gps locked onto the answer key",
        "flapping harder than usual tonight",
        "this nest is getting an upgrade",
        "one wing on the textbook one on coffee",
        "pecked through the hard part",
        "smooth glide through that problem set",
        "my plumage says casual but my notes say serious",
        "found a warm draft and rode it to page 200",
        "studying like the breadcrumbs depend on it",
        "today i am not a city pigeon i am a scholar pigeon",
        "coasted through the easy stuff now its real",
        "fluffed my feathers and got to work",
        "the nest is organized the mind is next",
        "caught an updraft of motivation",
        "feathers aligned brain calibrated",
        "drafting through this material like a tail wind",
        "the pigeon glow up is academic",
        "migrated from confusion to clarity",
        "nest upgrade: added a bookshelf",
        "preened my notes and they look good",
        "the flock doesnt know how hard i go",
        "wind beneath my wings is caffeine",
        "pecking above my weight class tonight",
        "the rooftop view of this chapter is beautiful",
        "pigeon gains are measured in pages",
        "flew in with questions leaving with answers",
        "my feathers are study-ready",
        "cooing with confidence after that section",
        "the thermal of knowledge carried me far",
        "upgraded my nest to include a whiteboard",
        "the early pigeon gets the study room",
        "pigeon stock is rising with every page",
        "gliding through these practice problems",
        "my beak is sharper and so is my mind",
        "the migration route goes through chapter seven",
    ].map { TaggedPun(text: $0, rarity: .uncommon) }
    
    // 30 rare puns
    private static let rarePuns: [TaggedPun] = [
        "carrier pigeon delivering knowledge",
        "this material is un-beak-lievable",
        "perched on the edge of understanding",
        "egg-cellent study session if i do say so myself",
        "birdbrained but still grinding",
        "landed a perfect study session",
        "no pigeon left behind",
        "the early pigeon gets the grade",
        "winging it and winning it",
        "flew over the nest of confusion",
        "coo-lumbus discovering new knowledge",
        "pigeon-holed into being productive",
        "this calls for a victory coo",
        "flew so high i can see the whole syllabus",
        "the rooftop view of knowledge hits different",
        "carrier pigeon status: delivering grades",
        "im not just winging it anymore this is flight",
        "found the golden breadcrumb in these notes",
        "the pigeon has entered the chat",
        "my wingspan just got wider with that chapter",
        "navigating by the stars and the study guide",
        "this pigeon just evolved a bigger brain",
        "from pigeon to professor in one session",
        "broke through the fog layer and found clarity",
        "the homing instinct led me straight to the answer",
        "feathers of steel nerves of a pigeon",
        "call me the bird of knowledge",
        "the pigeon council has reviewed and approved this session",
        "this isnt studying this is an ascension",
        "decoded the material like a carrier pigeon decodes routes",
    ].map { TaggedPun(text: $0, rarity: .rare) }
    
    // 15 epic puns
    private static let epicPuns: [TaggedPun] = [
        "homing pigeon locked onto that 4.0",
        "not all who waddle are lost",
        "from pigeon to phoenix this session",
        "the pigeon sees no limits only sky",
        "evolved from pigeon to owl tonight",
        "this nest runs on pure determination",
        "ascended past the flock tonight",
        "the council of pigeons would bow",
        "broke the sound barrier of studying",
        "they said pigeons cant read but look at me now",
        "rewrote the flight path and found the answer",
        "the pigeon throne is mine tonight",
        "crossed continents of knowledge in one sitting",
        "this pigeon has entered legendary airspace",
        "built the nest from scratch and its beautiful",
    ].map { TaggedPun(text: $0, rarity: .epic) }
    
    // 10 legendary puns
    private static let legendaryPuns: [TaggedPun] = [
        "the legendary pigeon of the library has appeared",
        "they will write tales of this study session",
        "a golden feather falls from the sky of knowledge",
        "the flock parts ways for the chosen pigeon",
        "a light shines down on this particular desk",
        "the pigeon of prophecy has opened the textbook",
        "scholars will reference this session for generations",
        "a single coo echoes and the universe listens",
        "the winds of fate have carried this pigeon to greatness",
        "the last pigeon standing and the textbook is finished",
    ].map { TaggedPun(text: $0, rarity: .legendary) }
    
    // 5 mythic puns
    private static let mythicPuns: [TaggedPun] = [
        "an ancient coo echoes through the halls of wisdom",
        "the mythic pigeon transcends all understanding",
        "the cosmos rearranged itself around this study session",
        "a pigeon of myth and legend walks among us tonight",
        "the sacred breadcrumb of infinite knowledge has been found",
    ].map { TaggedPun(text: $0, rarity: .mythic) }
    
    /// Roll a random pun using the gacha system, avoiding the last rolled index.
    static func rollPun(excluding lastIndex: Int) -> (pun: TaggedPun, index: Int) {
        let tier = PunRarity.roll()
        let punsInTier = allPuns.filter { $0.rarity == tier }
        guard !punsInTier.isEmpty else {
            return (allPuns[0], 0)
        }
        var pun: TaggedPun
        var index: Int
        repeat {
            pun = punsInTier.randomElement()!
            index = allPuns.firstIndex(where: { $0.text == pun.text }) ?? 0
        } while index == lastIndex && punsInTier.count > 1
        return (pun, index)
    }
    
    /// Look up the rarity for a known pun text. Returns nil for custom descriptions.
    static func rarity(for text: String) -> PunRarity? {
        allPuns.first(where: { $0.text == text })?.rarity
    }
}
