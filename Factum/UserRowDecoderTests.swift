//
//  UserRowDecoderTests.swift
//  Pigeon
//
//  Validates that UserRow decodes correctly without an email column.
//  This file runs at app launch in DEBUG builds only.
//

import Foundation

#if DEBUG
enum UserRowDecoderTests {
    /// Call once at launch to verify UserRow survives the post-migration schema
    /// where `email` is absent. Crashes in DEBUG if the decoder fails.
    static func runAll() {
        testDecodesWithoutEmail()
        testDecodesWithEmailKeyPresent()
        print("[TEST] UserRow decoder tests passed")
    }

    // MARK: - The critical test: no email key at all

    private static func testDecodesWithoutEmail() {
        let json = """
        {
            "uid": "12345678-1234-1234-1234-123456789abc",
            "display_name": "Test User",
            "username": "testuser",
            "avatar_url": null,
            "bio": "Hello world",
            "join_date": "2025-01-15T10:30:00+00:00",
            "total_study_minutes": 1200,
            "streak_days": 14,
            "friend_uids": ["uid1", "uid2"],
            "pending_friend_request_uids": [],
            "subjects": [{"name": "Math", "colorHex": "#FF0000"}],
            "last_seen_at": "2025-06-01T12:00:00+00:00",
            "is_studying": false,
            "current_subject": null,
            "is_private": false
        }
        """.data(using: .utf8)!

        let decoder = makeDecoder()
        do {
            let row = try decoder.decode(UserRow.self, from: json)
            assert(row.displayName == "Test User", "displayName mismatch")
            assert(row.username == "testuser", "username mismatch")
            assert(row.totalStudyMinutes == 1200, "totalStudyMinutes mismatch")
            assert(row.streakDays == 14, "streakDays mismatch")
            assert(row.friendUids == ["uid1", "uid2"], "friendUids mismatch")
            assert(row.isPrivate == false, "isPrivate mismatch")
        } catch {
            fatalError("[TEST] UserRow decode WITHOUT email failed: \(error)")
        }
    }

    // MARK: - Backwards compat: email key present but ignored

    private static func testDecodesWithEmailKeyPresent() {
        let json = """
        {
            "uid": "12345678-1234-1234-1234-123456789abc",
            "display_name": "Another User",
            "username": null,
            "email": "should-be-ignored@example.com",
            "avatar_url": "https://example.com/avatar.png",
            "bio": "",
            "join_date": "2025-01-15T10:30:00+00:00",
            "total_study_minutes": 0,
            "streak_days": 0,
            "friend_uids": [],
            "pending_friend_request_uids": [],
            "subjects": [],
            "last_seen_at": null,
            "is_studying": true,
            "current_subject": "Math",
            "is_private": true
        }
        """.data(using: .utf8)!

        let decoder = makeDecoder()
        do {
            let row = try decoder.decode(UserRow.self, from: json)
            assert(row.displayName == "Another User", "displayName mismatch")
            assert(row.isPrivate == true, "isPrivate mismatch")
            assert(row.isStudying == true, "isStudying mismatch")
            assert(row.currentSubject == "Math", "currentSubject mismatch")
        } catch {
            fatalError("[TEST] UserRow decode WITH email present failed: \(error)")
        }
    }

    // MARK: - Helpers

    private static func makeDecoder() -> JSONDecoder {
        let decoder = JSONDecoder()
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        decoder.dateDecodingStrategy = .custom { dec in
            let container = try dec.singleValueContainer()
            let dateString = try container.decode(String.self)
            guard let date = formatter.date(from: dateString) else {
                throw DecodingError.dataCorruptedError(
                    in: container,
                    debugDescription: "Cannot decode date: \(dateString)"
                )
            }
            return date
        }
        return decoder
    }
}
#endif
