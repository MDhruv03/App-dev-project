package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.ApplicationDao;
import com.example.myapplication.database.PendingSyncOperationDao;
import com.example.myapplication.model.Application;
import com.example.myapplication.model.PendingSyncOperation;
import com.example.myapplication.network.ApiCallback;
import com.example.myapplication.network.MockApiService;
import com.example.myapplication.network.NetworkUtils;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationRepository {

    private static final String ENTITY_APPLICATION = "application";
    private static final String OP_INSERT = "insert";
    private static final String OP_UPDATE = "update";
    private static final String OP_DELETE = "delete";
    private static final int MAX_RETRIES = 5;
    private static final int FLUSH_BATCH_SIZE = 25;
    
    private final Context appContext;
    private final ApplicationDao applicationDao;
    private final PendingSyncOperationDao pendingSyncDao;
    private final MockApiService apiService;
    private final ExecutorService executorService;
    private final Gson gson;
    
    public ApplicationRepository(Context context) {
        appContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        applicationDao = database.applicationDao();
        pendingSyncDao = database.pendingSyncOperationDao();
        apiService = MockApiService.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        gson = new Gson();
    }
    
    // Write-through insert: API first when online, local fallback offline
    public void insert(Application application, OnOperationCompleteListener listener) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            executorService.execute(() -> {
                long id = applicationDao.insert(application);
                enqueuePendingOperation(OP_INSERT, application, "offline");
                if (listener != null) {
                    listener.onComplete(id > 0);
                }
            });
            return;
        }

        apiService.addApplication(application, new ApiCallback<Application>() {
            @Override
            public void onSuccess(Application synced) {
                executorService.execute(() -> {
                    long id = applicationDao.upsert(synced);
                    if (listener != null) {
                        listener.onComplete(id > 0);
                    }
                });
            }

            @Override
            public void onError(String error) {
                executorService.execute(() -> {
                    long id = applicationDao.insert(application);
                    enqueuePendingOperation(OP_INSERT, application, error);
                    if (listener != null) {
                        listener.onComplete(id > 0);
                    }
                });
            }
        });
    }

    // Write-through update: API first when online, local fallback offline
    public void update(Application application, OnOperationCompleteListener listener) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            executorService.execute(() -> {
                applicationDao.update(application);
                enqueuePendingOperation(OP_UPDATE, application, "offline");
                if (listener != null) {
                    listener.onComplete(true);
                }
            });
            return;
        }

        apiService.updateApplication(application, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                executorService.execute(() -> {
                    applicationDao.update(application);
                    if (listener != null) {
                        listener.onComplete(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                executorService.execute(() -> {
                    applicationDao.update(application);
                    enqueuePendingOperation(OP_UPDATE, application, error);
                    if (listener != null) {
                        listener.onComplete(true);
                    }
                });
            }
        });
    }

    // Write-through delete: API first when online, local fallback offline
    public void delete(Application application, OnOperationCompleteListener listener) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            executorService.execute(() -> {
                applicationDao.delete(application);
                enqueuePendingOperation(OP_DELETE, application, "offline");
                if (listener != null) {
                    listener.onComplete(true);
                }
            });
            return;
        }

        apiService.deleteApplication(application.getId(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                executorService.execute(() -> {
                    applicationDao.delete(application);
                    if (listener != null) {
                        listener.onComplete(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                executorService.execute(() -> {
                    applicationDao.delete(application);
                    enqueuePendingOperation(OP_DELETE, application, error);
                    if (listener != null) {
                        listener.onComplete(true);
                    }
                });
            }
        });
    }

    // Network-first with local fallback
    public void getAllApplications(int userId, OnApplicationsLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            loadAllFromLocal(userId, listener, "No internet connection. Showing cached applications.");
            return;
        }

        flushPendingSyncOperations();

        apiService.fetchApplications(userId, new com.example.myapplication.network.ApiCallback<List<Application>>() {
            @Override
            public void onSuccess(List<Application> remoteApplications) {
                executorService.execute(() -> {
                    if (remoteApplications == null || remoteApplications.isEmpty()) {
                        loadAllFromLocal(userId, listener, null);
                        return;
                    }

                    mergeAndPersist(userId, remoteApplications);
                    List<Application> synced = applicationDao.getAllApplications(userId);
                    if (listener != null) {
                        listener.onLoaded(synced);
                    }
                });
            }

            @Override
            public void onError(String error) {
                loadAllFromLocal(userId, listener, "Failed to sync latest applications. Showing cached data.");
            }
        });
    }

    // Get applications by status
    public void getApplicationsByStatus(int userId, String status, OnApplicationsLoadedListener listener) {
        executorService.execute(() -> {
            List<Application> applications = applicationDao.getApplicationsByStatus(userId, status);
            if (listener != null) {
                listener.onLoaded(applications);
            }
        });
    }
    
    // Get total applications count
    public void getTotalApplications(int userId, OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = applicationDao.getTotalApplications(userId);
            if (listener != null) {
                listener.onLoaded(count);
            }
        });
    }
    
    // Get count by status
    public void getApplicationCountByStatus(int userId, String status, OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = applicationDao.getApplicationCountByStatus(userId, status);
            if (listener != null) {
                listener.onLoaded(count);
            }
        });
    }

    // Get pending sync count for application operations
    public void getPendingSyncCount(OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = pendingSyncDao.getPendingCountByEntityType(ENTITY_APPLICATION);
            if (listener != null) {
                listener.onLoaded(count);
            }
        });
    }

    public void getPendingSyncBreakdown(OnPendingSyncBreakdownListener listener) {
        executorService.execute(() -> {
            int insertCount = pendingSyncDao.getPendingCountByEntityAndOperation(ENTITY_APPLICATION, OP_INSERT);
            int updateCount = pendingSyncDao.getPendingCountByEntityAndOperation(ENTITY_APPLICATION, OP_UPDATE);
            int deleteCount = pendingSyncDao.getPendingCountByEntityAndOperation(ENTITY_APPLICATION, OP_DELETE);
            if (listener != null) {
                listener.onLoaded(insertCount, updateCount, deleteCount);
            }
        });
    }

    public void syncPendingOperations() {
        flushPendingSyncOperations();
    }
    
    // Interfaces for callbacks
    public interface OnApplicationsLoadedListener {
        void onLoaded(List<Application> applications);

        default void onError(String message) {
        }
    }
    
    public interface OnOperationCompleteListener {
        void onComplete(boolean success);
    }
    
    public interface OnCountLoadedListener {
        void onLoaded(int count);
    }

    public interface OnPendingSyncBreakdownListener {
        void onLoaded(int insertCount, int updateCount, int deleteCount);
    }

    private void loadAllFromLocal(int userId, OnApplicationsLoadedListener listener, String fallbackMessage) {
        executorService.execute(() -> {
            List<Application> applications = applicationDao.getAllApplications(userId);
            if (listener != null) {
                if (fallbackMessage != null && !fallbackMessage.trim().isEmpty()) {
                    listener.onError(fallbackMessage);
                }
                listener.onLoaded(applications);
            }
        });
    }

    private void mergeAndPersist(int userId, List<Application> remoteApplications) {
        List<Application> localApplications = applicationDao.getAllApplications(userId);
        ApplicationSyncHelper.mergeLocalStateIntoRemote(userId, remoteApplications, localApplications);

        applicationDao.deleteAllForUser(userId);
        applicationDao.insertAll(remoteApplications);
    }

    private void enqueuePendingOperation(String operationType, Application application, String error) {
        PendingSyncOperation pending = new PendingSyncOperation();
        pending.setEntityType(ENTITY_APPLICATION);
        pending.setOperationType(operationType);
        pending.setPayloadJson(gson.toJson(application));
        pending.setLastError(error);
        pendingSyncDao.insert(pending);
    }

    private void flushPendingSyncOperations() {
        executorService.execute(() -> {
            if (!NetworkUtils.isNetworkAvailable(appContext)) {
                return;
            }

            List<PendingSyncOperation> operations = pendingSyncDao.getPendingOperations(FLUSH_BATCH_SIZE);
            for (PendingSyncOperation operation : operations) {
                if (!ENTITY_APPLICATION.equals(operation.getEntityType())) {
                    continue;
                }

                Application payload = gson.fromJson(operation.getPayloadJson(), Application.class);
                if (payload == null) {
                    pendingSyncDao.deleteById(operation.getId());
                    continue;
                }

                switch (operation.getOperationType()) {
                    case OP_INSERT:
                        apiService.addApplication(payload, new ApiCallback<Application>() {
                            @Override
                            public void onSuccess(Application result) {
                                executorService.execute(() -> {
                                    applicationDao.upsert(result);
                                    pendingSyncDao.deleteById(operation.getId());
                                });
                            }

                            @Override
                            public void onError(String error) {
                                executorService.execute(() -> pendingSyncDao.markRetryFailed(operation.getId(), error));
                            }
                        });
                        break;
                    case OP_UPDATE:
                        apiService.updateApplication(payload, new ApiCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                executorService.execute(() -> pendingSyncDao.deleteById(operation.getId()));
                            }

                            @Override
                            public void onError(String error) {
                                executorService.execute(() -> pendingSyncDao.markRetryFailed(operation.getId(), error));
                            }
                        });
                        break;
                    case OP_DELETE:
                        apiService.deleteApplication(payload.getId(), new ApiCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                executorService.execute(() -> pendingSyncDao.deleteById(operation.getId()));
                            }

                            @Override
                            public void onError(String error) {
                                executorService.execute(() -> pendingSyncDao.markRetryFailed(operation.getId(), error));
                            }
                        });
                        break;
                    default:
                        pendingSyncDao.deleteById(operation.getId());
                        break;
                }
            }

            pendingSyncDao.deleteExhaustedRetries(MAX_RETRIES);
        });
    }
}
