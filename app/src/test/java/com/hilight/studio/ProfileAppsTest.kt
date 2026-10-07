package com.hilight.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAppsTest {
    private val raw = """
        user:0
        package:/data/app/~~aa==/com.mail-bb==/base.apk=com.mail
        user:10
        package:/data/app/~~cc==/com.work-dd==/base.apk=com.work
        package:/data/app/~~aa==/com.mail-bb==/base.apk=com.mail
        user:11
        package:/data/app/~~ee==/com.secret-ff==/base.apk=com.secret
        garbage
    """.trimIndent()

    @Test fun parsesPathsContainingEqualsSigns() {
        val e = ProfileApps.parse(raw).first()
        assertEquals(ProfileApps.Entry(0, "com.mail", "/data/app/~~aa==/com.mail-bb==/base.apk"), e)
    }

    @Test fun otherProfilesExcludeOwnUserAndDuplicates() {
        val pkgs = ProfileApps.otherProfileEntries(raw, 0).map { it.userId to it.pkg }
        assertEquals(listOf(10 to "com.work", 10 to "com.mail", 11 to "com.secret"), pkgs)
    }
}
