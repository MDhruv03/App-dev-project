package com.example.myapplication.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.ai.ResumeParser;
import com.example.myapplication.model.UserProfile;
import com.example.myapplication.viewmodel.UserProfileViewModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_CODE_PICK_RESUME = 1001;

    private EditText etName;
    private EditText etEmail;
    private EditText etSkills;
    private EditText etPreferredRoles;
    private EditText etPreferredLocation;
    private TextView tvResumeSkills;
    private TextView tvResumeFileName;
    private Spinner spinnerJobType;
    private CheckBox cbPaidPreference;
    private Button btnSaveProfile;
    private Button btnUploadResume;

    private UserProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupSpinner();
        setupViewModel();
        setupResumeUpload();
        setupSaveAction();
    }

    private void initializeViews(View view) {
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etSkills = view.findViewById(R.id.etSkills);
        etPreferredRoles = view.findViewById(R.id.etPreferredRoles);
        etPreferredLocation = view.findViewById(R.id.etPreferredLocation);
        spinnerJobType = view.findViewById(R.id.spinnerJobType);
        cbPaidPreference = view.findViewById(R.id.cbPaidPreference);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        tvResumeSkills = view.findViewById(R.id.tvResumeSkills);
        tvResumeFileName = view.findViewById(R.id.tvResumeFileName);
        btnUploadResume = view.findViewById(R.id.btnUploadResume);
    }

    private void setupSpinner() {
        String[] jobTypes = new String[]{"any", "remote", "onsite"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, jobTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJobType.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) {
                return;
            }

            etName.setText(safe(profile.getName()));
            etEmail.setText(safe(profile.getEmail()));
            etSkills.setText(safe(profile.getSkills()));
            etPreferredRoles.setText(safe(profile.getPreferredRoles()));
            etPreferredLocation.setText(safe(profile.getPreferredLocation()));
            cbPaidPreference.setChecked(profile.isPaidPreference());

            String jobType = safe(profile.getJobTypePreference());
            if (!jobType.isEmpty()) {
                int position = -1;
                for (int i = 0; i < spinnerJobType.getCount(); i++) {
                    Object item = spinnerJobType.getItemAtPosition(i);
                    if (jobType.equals(String.valueOf(item))) {
                        position = i;
                        break;
                    }
                }
                if (position >= 0) {
                    spinnerJobType.setSelection(position);
                }
            }
        });
    }

    private void setupSaveAction() {
        btnSaveProfile.setOnClickListener(v -> {
            viewModel.saveProfile(buildProfileFromFields());
            Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupResumeUpload() {
        btnUploadResume.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Resume PDF"), REQUEST_CODE_PICK_RESUME);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_PICK_RESUME || resultCode != android.app.Activity.RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            return;
        }

        tvResumeFileName.setText(getFileName(uri));

        byte[] bytes = readAllBytes(uri);
        ResumeParser.ResumeSummary summary = ResumeParser.parseResumeFromBytes(bytes);
        List<String> extractedSkills = summary != null ? summary.getExtractedSkills() : null;

        if (extractedSkills == null || extractedSkills.isEmpty()) {
            tvResumeSkills.setText("No skills extracted");
            return;
        }

        String extractedText = joinSkills(extractedSkills);
        tvResumeSkills.setText(extractedText);

        String currentSkills = etSkills.getText() != null ? etSkills.getText().toString().trim() : "";
        if (currentSkills.isEmpty()) {
            etSkills.setText(extractedText);
        } else {
            etSkills.setText(currentSkills + ", " + extractedText);
        }

        viewModel.saveProfile(buildProfileFromFields());
    }

    private UserProfile buildProfileFromFields() {
        UserProfile profile = new UserProfile();
        profile.setId(1);
        profile.setName(etName.getText().toString().trim());
        profile.setEmail(etEmail.getText().toString().trim());
        profile.setSkills(etSkills.getText().toString().trim());
        profile.setPreferredRoles(etPreferredRoles.getText().toString().trim());
        profile.setPreferredLocation(etPreferredLocation.getText().toString().trim());
        profile.setJobTypePreference(String.valueOf(spinnerJobType.getSelectedItem()));
        profile.setPaidPreference(cbPaidPreference.isChecked());
        return profile;
    }

    private String getFileName(Uri uri) {
        String fallback = "resume.pdf";
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fallback;
    }

    private byte[] readAllBytes(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                return new byte[0];
            }

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Unable to read resume", Toast.LENGTH_SHORT).show();
            return new byte[0];
        }
    }

    private String joinSkills(List<String> skills) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(skills.get(i));
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
