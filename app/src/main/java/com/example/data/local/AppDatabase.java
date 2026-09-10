package com.example.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.data.local.dao.ConversationDao;
import com.example.data.local.dao.MediaAssetDao;
import com.example.data.local.dao.MediaJobDao;
import com.example.data.local.dao.MessageDao;
import com.example.data.local.dao.ModelDao;
import com.example.data.local.dao.ProbeLogDao;
import com.example.data.local.dao.ProviderDao;
import com.example.data.local.entity.ConversationEntity;
import com.example.data.local.entity.MediaAssetEntity;
import com.example.data.local.entity.MediaJobEntity;
import com.example.data.local.entity.MessageEntity;
import com.example.data.local.entity.ModelEntity;
import com.example.data.local.entity.ProbeLogEntity;
import com.example.data.local.entity.ProviderEntity;

@Database(
    entities = {
        ProviderEntity.class,
        ModelEntity.class,
        ConversationEntity.class,
        MessageEntity.class,
        MediaJobEntity.class,
        MediaAssetEntity.class,
        ProbeLogEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract ProviderDao providerDao();
    public abstract ModelDao modelDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();
    public abstract MediaJobDao mediaJobDao();
    public abstract MediaAssetDao mediaAssetDao();
    public abstract ProbeLogDao probeLogDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "omnichat.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
