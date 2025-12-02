package com.bytedance.xhsdemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Room 数据库入口：维护用户和用户资料两张表
@Database(
    entities = [UserEntity::class, UserProfileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val seedScope = CoroutineScope(Dispatchers.IO)

        // 懒加载单例获取数据库实例
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xhs-demo.db"
                )
                    // 简化迁移策略：版本不兼容时直接重建
                    .fallbackToDestructiveMigration()
                    // 在数据库首次创建时插入默认用户和默认资料
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default user and profile so first login works immediately.
                            seedScope.launch {
                                INSTANCE?.userDao()?.insertUser(
                                    UserEntity(
                                        phone = "13800138000",
                                        password = "123456"
                                    )
                                )
                                INSTANCE?.profileDao()?.insertProfile(
                                    UserProfileEntity(
                                        displayName = "Sherry ~",
                                        signature = "点击这里，填写简介",
                                        avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=200&q=60"
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
