package icons

import com.intellij.openapi.util.IconLoader

object StackFileSync {
    @JvmField
    val Logo = IconLoader.getIcon("/icons/logo.svg", StackFileSync::class.java)
    
    @JvmField
    val Sync = IconLoader.getIcon("/icons/sync.svg", StackFileSync::class.java)
    
    @JvmField
    val AutoSync = IconLoader.getIcon("/icons/auto-sync.svg", StackFileSync::class.java)
    
    @JvmField
    val AutoSyncDisabled = IconLoader.getIcon("/icons/auto-sync-disabled.svg", StackFileSync::class.java)
    
    @JvmField
    val Settings = IconLoader.getIcon("/icons/settings.svg", StackFileSync::class.java)
} 