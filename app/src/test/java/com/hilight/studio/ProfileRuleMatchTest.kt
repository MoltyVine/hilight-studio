package com.hilight.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRuleMatchTest {
    private val any = AppRule(pkg = "com.mail", label = "Mail", stableId = "any")
    private val work = AppRule(pkg = "com.mail", label = "Mail · Work", profileId = 10, stableId = "work")
    private fun notif(user: Int) = MessageInfo(pkg = "com.mail", userId = user)

    @Test fun profileRuleWinsOnlyForItsOwnProfile() {
        assertEquals("work", ConversationMatch.resolve(listOf(any, work), notif(10))?.id)
        assertEquals("any", ConversationMatch.resolve(listOf(any, work), notif(0))?.id)
        assertEquals("any", ConversationMatch.resolve(listOf(work, any), notif(0))?.id)
    }

    @Test fun profileRuleIgnoresOtherProfiles() {
        assertNull(ConversationMatch.resolve(listOf(work), notif(0)))
        assertNull(ConversationMatch.resolve(listOf(work), notif(11)))
    }

    @Test fun idsStayLegacyForMainProfileAndDifferPerProfile() {
        val a = AppRule(pkg = "p", label = "p")
        val b = a.copy(profileId = 10)
        assertEquals("p|NOTIFICATION|", a.id)
        assertEquals("p|NOTIFICATION||u10", b.id)
        assertEquals(10, AppRule.fromJson(b.toPrefsJson()).profileId)
        assertNull(AppRule.fromJson(a.toPrefsJson()).profileId)
    }
}
