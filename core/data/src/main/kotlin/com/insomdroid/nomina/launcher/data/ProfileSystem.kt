package com.insomdroid.nomina.data

import android.os.Build
import kotlinx.coroutines.flow.Flow

/** How the platform types a user profile, as far as the public API tells a launcher. */
enum class ProfileKind {
    /** The launcher's own user. */
    Own,
    /** A managed (work) profile — API 35+ `UserManager.USER_TYPE_PROFILE_MANAGED`. */
    Managed,
    /** Android 15 Private Space — API 35+ `USER_TYPE_PROFILE_PRIVATE`. */
    Private,
    /** An app-cloning profile — API 35+ `USER_TYPE_PROFILE_CLONE`. */
    Clone,
    /** Not typed by the platform: API 30–34, or a profile `LauncherApps.getLauncherUserInfo` does not
     *  describe. The badge check decides then (see [workProfileOf]). */
    Unknown,
}

/**
 * One profile as the platform reports it right now. [badged] is Launcher3's `ApiWrapper.queryAllUsers`
 * check — `PackageManager.getUserBadgedIcon` returns a different drawable for a badged profile — and
 * is what tells a work profile from the own user where [kind] is [ProfileKind.Unknown]. [paused] is
 * the profile's quiet mode.
 */
data class ProfileSnapshot(val serial: Long, val kind: ProfileKind, val badged: Boolean, val paused: Boolean)

/**
 * The platform's profile knowledge behind the drawer's Work tab, as an interface so the decision and
 * the repository are testable with a fake. The launcher is the device HOME, so [snapshot] never
 * throws: a lookup that fails answers an empty list.
 */
interface ProfileSystem {
    /** Every profile the launcher can see, the own user included; empty when the lookup fails. */
    fun snapshot(): List<ProfileSnapshot>

    /** One `Unit` right after the receiver is registered — a request to re-read, because a profile
     *  that changed before the receiver existed sent a broadcast nobody heard — then one per profile
     *  broadcast (added / removed / available / unavailable / unlocked). Cold: each collector
     *  registers its own receiver, so share it. */
    fun changes(): Flow<Unit>

    /** `UserManager.requestQuietModeEnabled` for the profile with [serial]; throws what the platform
     *  throws (a `SecurityException` when the app is not the default launcher). */
    fun requestQuietMode(serial: Long, enabled: Boolean): Boolean
}

/**
 * Whether a profile's launchable apps may enter Nomina's shared app list.
 *
 * Android 15+ identifies managed, private and clone profiles. Only positively identified
 * non-private profiles are listed; an unknown or unavailable profile is excluded because it
 * may be a Private Space that this launcher does not implement. Before Android 15, profile
 * types are not exposed through LauncherApps, so retain the previous all-profiles behavior.
 */
internal fun shouldListProfileApps(
    isOwnUser: Boolean,
    serial: Long?,
    kinds: Map<Long, ProfileKind>,
    sdkInt: Int,
): Boolean = when {
    isOwnUser -> true
    sdkInt < Build.VERSION_CODES.VANILLA_ICE_CREAM -> true
    serial == null -> false
    else -> when (kinds[serial]) {
        ProfileKind.Own, ProfileKind.Managed, ProfileKind.Clone -> true
        ProfileKind.Private, ProfileKind.Unknown, null -> false
    }
}

/**
 * The one profile the Work tab shows, or null: a [ProfileKind.Managed] profile, or an
 * [ProfileKind.Unknown] one that is badged (API 30–34 cannot tell a clone profile apart, and
 * Launcher3 lumps them together there too). The own user, private and clone profiles never
 * qualify. Exactly one work profile is supported: the lowest serial wins.
 */
fun workProfileOf(profiles: List<ProfileSnapshot>): ProfileSnapshot? = profiles
    .filter { it.kind == ProfileKind.Managed || (it.kind == ProfileKind.Unknown && it.badged) }
    .minByOrNull { it.serial }
