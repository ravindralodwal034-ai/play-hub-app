package com.example.data.local

object LocalChannelRoadmapGenerator {

    fun generate12WeekRoadmap(
        channelId: String,
        channelName: String,
        niche: String,
        language: String,
        weeklyVideoTarget: Int,
        weeklyShortsTarget: Int
    ): List<RoadmapMilestoneEntity> {
        val totalWeeklyTarget = (weeklyVideoTarget + weeklyShortsTarget).coerceAtLeast(1)
        val nicheLabel = niche.ifBlank { "Creator" }
        val langLabel = language.ifBlank { "Global" }

        val specs = listOf(
            // Month 1: Foundation, Identity & First Uploads (Weeks 1-4)
            WeekSpec(
                month = 1,
                week = 1,
                title = "Brand Setup & Channel Trailer",
                guide = "Finalize $channelName logo, banner, bio in $langLabel, and upload a crisp 45-second channel intro introducing your $nicheLabel vibe."
            ),
            WeekSpec(
                month = 1,
                week = 2,
                title = "First $nicheLabel Batch & Hook Practice",
                guide = "Record and publish your first episodes using free built-in editors like Photopea (thumbnails) and Kdenlive. Hook viewers in the first 3 seconds."
            ),
            WeekSpec(
                month = 1,
                week = 3,
                title = "Daily Shorts Discovery Sprint",
                guide = "Clip highlights from your main $nicheLabel videos into vertical Shorts in $langLabel with clear captions to attract initial subscribers."
            ),
            WeekSpec(
                month = 1,
                week = 4,
                title = "Month 1 Review & Playlist Architecture",
                guide = "Group your $nicheLabel uploads into themed playlists so binge-watchers automatically move to your next video."
            ),
            // Month 2: Consistency, Retention & Community (Weeks 5-8)
            WeekSpec(
                month = 2,
                week = 5,
                title = "Thumbnail & Title A/B Refresh",
                guide = "Upgrade your top 3 video titles and thumbnails in Photopea for high click-through clarity and bold $langLabel storytelling."
            ),
            WeekSpec(
                month = 2,
                week = 6,
                title = "Audience Q&A + Requested Topics",
                guide = "Reply to every viewer comment and publish one requested $nicheLabel video or folk/music remix requested by your early fans."
            ),
            WeekSpec(
                month = 2,
                week = 7,
                title = "Repeatable Weekly Signature Series",
                guide = "Lock in a consistent upload day & time for a recurring $nicheLabel series in $langLabel so subscribers return every week."
            ),
            WeekSpec(
                month = 2,
                week = 8,
                title = "Month 2 Retention & Pacing Tune-Up",
                guide = "Check which moments viewers replayed most, tighten intros to under 10 seconds, and cross-link Shorts to long-form episodes."
            ),
            // Month 3: Scale, Collaborations & Monetization Readiness (Weeks 9-12)
            WeekSpec(
                month = 3,
                week = 9,
                title = "Creator Collaboration & Shoutouts",
                guide = "Team up with another $langLabel or $nicheLabel creator for a duet, medley, or split-screen episode to cross-pollinate audiences."
            ),
            WeekSpec(
                month = 3,
                week = 10,
                title = "Flagship 10-Minute Special Episode",
                guide = "Produce your highest-effort flagship $nicheLabel showcase using multi-track edits, crisp audio mastering, and chapter markers."
            ),
            WeekSpec(
                month = 3,
                week = 11,
                title = "Community Polls & Live Premiere",
                guide = "Schedule a live premiere or community premiere chat for your $channelName fans and promote it across your Shorts feed."
            ),
            WeekSpec(
                month = 3,
                week = 12,
                title = "90-Day Milestone & Next-Quarter Scale",
                guide = "Audit your 90-day views, top performing $langLabel topics, and monetization eligibility checklist to plan Months 4–6."
            )
        )

        return specs.map { spec ->
            RoadmapMilestoneEntity(
                id = "${channelId}_w${spec.week}",
                channelId = channelId,
                monthNumber = spec.month,
                weekNumber = spec.week,
                title = spec.title,
                guide = spec.guide,
                targetUploads = totalWeeklyTarget,
                completedUploads = 0,
                isCompleted = false
            )
        }
    }

    private data class WeekSpec(
        val month: Int,
        val week: Int,
        val title: String,
        val guide: String
    )
}
