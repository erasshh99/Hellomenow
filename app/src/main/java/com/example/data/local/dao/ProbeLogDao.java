package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.data.local.entity.ProbeLogEntity;
import java.util.List;

@Dao
public interface ProbeLogDao {
    @Query("SELECT * FROM probe_logs WHERE modelId = :modelId ORDER BY timestamp DESC")
    LiveData<List<ProbeLogEntity>> getLogsForModel(String modelId);

    @Insert
    void insert(ProbeLogEntity log);

    @Query("DELETE FROM probe_logs WHERE modelId = :modelId")
    void deleteForModel(String modelId);
}
