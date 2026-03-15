package com.example.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.ApplicationDao;
import com.example.myapplication.database.PendingSyncOperationDao;
import com.example.myapplication.model.Application;
import com.example.myapplication.model.PendingSyncOperation;
import com.example.myapplication.repository.ApplicationRepository;
import com.example.myapplication.repository.ApplicationSyncHelper;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SyncWorker extends Worker {

    private static final int BATCH_SIZE = 50;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(appContext);

        // Repository participates in actual sync orchestration against pending operations.
        ApplicationRepository repository = new ApplicationRepository(appContext);

        // Inspect pending operations and normalize payload keys through helper utilities.
        ApplicationDao applicationDao = database.applicationDao();
        PendingSyncOperationDao pendingSyncDao = database.pendingSyncOperationDao();
        List<PendingSyncOperation> pendingOperations = pendingSyncDao.getPendingOperations(BATCH_SIZE);
        Gson gson = new Gson();
        for (PendingSyncOperation operation : pendingOperations) {
            if (!"application".equals(operation.getEntityType())) {
                continue;
            }

            Application payload = gson.fromJson(operation.getPayloadJson(), Application.class);
            if (payload == null) {
                continue;
            }

                List<Application> localApplications = applicationDao.getAllApplications(payload.getUserId());
            ApplicationSyncHelper.mergeLocalStateIntoRemote(
                    payload.getUserId(),
                    new ArrayList<>(java.util.Collections.singletonList(payload)),
                    localApplications
            );
        }

        repository.syncPendingOperations();
        return Result.success();
    }
}
