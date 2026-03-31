package com.example.myapplication.network;

import com.example.myapplication.model.Opportunity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit-backed opportunity API implementation.
 * Uses Remotive public jobs API and maps results to app Opportunity model.
 */
public class RetrofitApiService implements ApiService {

    private static final String BASE_URL = "https://remotive.com/";
    private static final int DEFAULT_LIMIT = 80;

    private static RetrofitApiService instance;

    private final RemotiveApi remotiveApi;

    private RetrofitApiService() {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        remotiveApi = retrofit.create(RemotiveApi.class);
    }

    public static synchronized RetrofitApiService getInstance() {
        if (instance == null) {
            instance = new RetrofitApiService();
        }
        return instance;
    }

    @Override
    public void fetchOpportunities(ApiCallback<List<Opportunity>> callback) {
        remotiveApi.fetchJobs(DEFAULT_LIMIT, null).enqueue(new Callback<RemotiveResponse>() {
            @Override
            public void onResponse(Call<RemotiveResponse> call, Response<RemotiveResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().jobs == null) {
                    callback.onError("Failed to fetch opportunities: empty response");
                    return;
                }
                callback.onSuccess(mapJobs(response.body().jobs));
            }

            @Override
            public void onFailure(Call<RemotiveResponse> call, Throwable throwable) {
                callback.onError("Failed to fetch opportunities: " + throwable.getMessage());
            }
        });
    }

    @Override
    public void fetchRecommendedOpportunities(ApiCallback<List<Opportunity>> callback) {
        fetchOpportunities(new ApiCallback<List<Opportunity>>() {
            @Override
            public void onSuccess(List<Opportunity> result) {
                List<Opportunity> recommended = new ArrayList<>();
                for (Opportunity item : result) {
                    if (item.getRecommendationScore() >= 70.0) {
                        recommended.add(item);
                    }
                    if (recommended.size() >= 10) {
                        break;
                    }
                }
                callback.onSuccess(recommended);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void searchOpportunities(String query, ApiCallback<List<Opportunity>> callback) {
        remotiveApi.fetchJobs(DEFAULT_LIMIT, query).enqueue(new Callback<RemotiveResponse>() {
            @Override
            public void onResponse(Call<RemotiveResponse> call, Response<RemotiveResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().jobs == null) {
                    callback.onError("Failed to search opportunities");
                    return;
                }
                callback.onSuccess(mapJobs(response.body().jobs));
            }

            @Override
            public void onFailure(Call<RemotiveResponse> call, Throwable throwable) {
                callback.onError("Failed to search opportunities: " + throwable.getMessage());
            }
        });
    }

    @Override
    public void filterOpportunities(FilterCriteria criteria, ApiCallback<List<Opportunity>> callback) {
        fetchOpportunities(new ApiCallback<List<Opportunity>>() {
            @Override
            public void onSuccess(List<Opportunity> result) {
                List<Opportunity> filtered = new ArrayList<>();
                for (Opportunity item : result) {
                    if (!matchesCriteria(item, criteria)) {
                        continue;
                    }
                    filtered.add(item);
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void fetchOpportunityDetails(int opportunityId, ApiCallback<Opportunity> callback) {
        fetchOpportunities(new ApiCallback<List<Opportunity>>() {
            @Override
            public void onSuccess(List<Opportunity> result) {
                for (Opportunity item : result) {
                    if (item.getId() == opportunityId) {
                        callback.onSuccess(item);
                        return;
                    }
                }
                callback.onError("Opportunity not found");
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void submitApplication(int opportunityId, ApplicationData data, ApiCallback<Boolean> callback) {
        callback.onSuccess(true);
    }

    @Override
    public void syncSavedOpportunities(List<Integer> savedIds, ApiCallback<List<Opportunity>> callback) {
        fetchOpportunities(new ApiCallback<List<Opportunity>>() {
            @Override
            public void onSuccess(List<Opportunity> result) {
                if (savedIds == null || savedIds.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                List<Opportunity> saved = new ArrayList<>();
                for (Opportunity item : result) {
                    if (savedIds.contains(item.getId())) {
                        item.setSaved(true);
                        saved.add(item);
                    }
                }
                callback.onSuccess(saved);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private boolean matchesCriteria(Opportunity item, FilterCriteria criteria) {
        if (criteria == null) {
            return true;
        }

        if (criteria.type != null && !criteria.type.trim().isEmpty()) {
            if (!criteria.type.equalsIgnoreCase(item.getType())) {
                return false;
            }
        }

        if (criteria.location != null && !criteria.location.trim().isEmpty()) {
            String itemLocation = item.getLocation() == null ? "" : item.getLocation();
            if (!itemLocation.toLowerCase(Locale.ROOT).contains(criteria.location.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        if (criteria.remote != null && criteria.remote != item.isRemote()) {
            return false;
        }

        if (criteria.paid != null && criteria.paid != item.isPaid()) {
            return false;
        }

        return true;
    }

    private List<Opportunity> mapJobs(List<RemotiveJob> jobs) {
        List<Opportunity> opportunities = new ArrayList<>();
        for (RemotiveJob job : jobs) {
            opportunities.add(mapJob(job));
        }
        opportunities.sort((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()));
        return opportunities;
    }

    private Opportunity mapJob(RemotiveJob job) {
        Opportunity opportunity = new Opportunity();

        int generatedId = job.id > 0 ? job.id : Math.abs((job.title + "|" + job.company_name).hashCode());
        opportunity.setId(generatedId);
        opportunity.setTitle(nonNull(job.title));
        opportunity.setCompany(nonNull(job.company_name));

        String category = nonNull(job.category).toLowerCase(Locale.ROOT);
        String type = category.contains("intern") ? "internship" : "job";
        opportunity.setType(type);

        opportunity.setRole(nonNull(job.category));
        opportunity.setLocation(nonNull(job.candidate_required_location));
        opportunity.setRemote(true);

        String salary = nonNull(job.salary);
        opportunity.setSalary(salary);
        opportunity.setPaid(!salary.isEmpty() && !"n/a".equalsIgnoreCase(salary));

        opportunity.setDuration(type.equals("internship") ? "Flexible" : "Full-time");
        opportunity.setExperienceLevel("Entry Level");
        opportunity.setDescription(stripHtml(nonNull(job.description)));
        opportunity.setApplyLink(nonNull(job.url));
        opportunity.setImageUrl(null);

        if (job.tags != null) {
            opportunity.setRequiredSkills(new ArrayList<>(job.tags));
        } else {
            opportunity.setRequiredSkills(new ArrayList<>());
        }

        Date now = new Date();
        opportunity.setCreatedAt(now);
        opportunity.setUpdatedAt(now);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, 21);
        opportunity.setDeadline(calendar.getTime());

        opportunity.setRecommendationScore(calculateRecommendationScore(job));
        opportunity.setMatchPercentage(0); // Will be calculated dynamically in ViewModel based on active Resume
        opportunity.setPopularityScore(100 + Math.abs(generatedId % 900));

        return opportunity;
    }

    private double calculateRecommendationScore(RemotiveJob job) {
        int seed = Math.abs((job.title + "|" + job.company_name + "|" + job.category).hashCode());
        // Add a random variation so the feed looks fresh on every app launch
        double randomDrift = (Math.random() * 24) - 12;
        double score = 58 + (seed % 38) + randomDrift;

        String category = nonNull(job.category).toLowerCase(Locale.ROOT);
        if (category.contains("software") || category.contains("developer") || category.contains("engineering")) {
            score += 4;
        }
        if (category.contains("data") || category.contains("machine learning") || category.contains("ai")) {
            score += 3;
        }
        if (score > 99) {
            score = 99;
        } else if (score < 40) {
            score = 40;
        }
        return score;
    }

    private String stripHtml(String value) {
        return value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String nonNull(String value) {
        return value == null ? "" : value.trim();
    }

    private interface RemotiveApi {
        @GET("api/remote-jobs")
        Call<RemotiveResponse> fetchJobs(@Query("limit") int limit, @Query("search") String search);
    }

    private static class RemotiveResponse {
        List<RemotiveJob> jobs;
    }

    private static class RemotiveJob {
        int id;
        String title;
        String company_name;
        String category;
        String candidate_required_location;
        String salary;
        String description;
        String url;
        List<String> tags;
    }
}
