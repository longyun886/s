package com.example.filerenamer;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etSuffix, etPath;
    private Button btnRename, btnRestore;
    private CheckBox cbAutoClassify, cbAutoExecute;
    
    private Map<String, String> originalNames = new HashMap<>();
    
    private static final int STORAGE_PERMISSION_CODE = 100;
    
    private String currentSuffix = "xaxbxc";
    private String currentPath = "dcim/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etSuffix = findViewById(R.id.etSuffix);
        etPath = findViewById(R.id.etPath);
        btnRename = findViewById(R.id.btnRename);
        btnRestore = findViewById(R.id.btnRestore);
        cbAutoClassify = findViewById(R.id.cbAutoClassify);
        cbAutoExecute = findViewById(R.id.cbAutoExecute);

        updateSuffixDisplay(false);
        updatePathDisplay(false);

        etSuffix.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etSuffix.setText(currentSuffix);
            } else {
                updateSuffixDisplay(false);
            }
        });

        etPath.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etPath.setText(currentPath);
            } else {
                updatePathDisplay(false);
            }
        });

        etSuffix.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSuffix = s.toString();
                if (cbAutoExecute.isChecked() && !currentSuffix.isEmpty()) {
                    performRename();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPath = s.toString();
                if (cbAutoExecute.isChecked() && !currentPath.isEmpty()) {
                    performRename();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cbAutoExecute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !currentSuffix.isEmpty() && !currentPath.isEmpty()) {
                performRename();
            }
        });

        btnRename.setOnClickListener(v -> performRename());

        btnRestore.setOnClickListener(v -> restoreFiles());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    private void updateSuffixDisplay(boolean isEditing) {
        if (isEditing) {
            etSuffix.setText(currentSuffix);
        } else {
            etSuffix.setText("." + currentSuffix);
        }
    }

    private void updatePathDisplay(boolean isEditing) {
        if (isEditing) {
            etPath.setText(currentPath);
        } else {
            etPath.setText("/sdcard/" + currentPath);
        }
    }

    private void performRename() {
        if (currentSuffix.isEmpty() || currentPath.isEmpty()) {
            Toast.makeText(this, "后缀和路径不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File targetDir = new File("/sdcard/" + currentPath);
        
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            Toast.makeText(this, "目录不存在: " + targetDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        File[] files = targetDir.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(this, "目录中没有文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean classifyEnabled = cbAutoClassify.isChecked();
        originalNames.clear();
        int renamedCount = 0;
        
        for (File file : files) {
            if (file.isFile()) {
                originalNames.put(file.getAbsolutePath(), file.getName());
                
                String newName;
                if (classifyEnabled) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            .format(new Date(file.lastModified()));
                    newName = timeStamp + "_" + file.getName() + "." + currentSuffix;
                } else {
                    newName = file.getName() + "." + currentSuffix;
                }
                
                File newFile = new File(targetDir, newName);
                if (file.renameTo(newFile)) {
                    renamedCount++;
                    
                    if (classifyEnabled) {
                        String category = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                .format(new Date(file.lastModified()));
                        
                        File categoryDir = new File(targetDir, category);
                        if (!categoryDir.exists()) {
                            categoryDir.mkdirs();
                        }
                        
                        File finalFile = new File(categoryDir, newName);
                        newFile.renameTo(finalFile);
                    }
                }
            }
        }
        
        Toast.makeText(this, 
                "成功重命名 " + renamedCount + " 个文件", 
                Toast.LENGTH_SHORT).show();
    }

    private void restoreFiles() {
        if (originalNames.isEmpty()) {
            Toast.makeText(this, "没有可还原的文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File targetDir = new File("/sdcard/" + currentPath);
        int restoredCount = 0;
        
        for (Map.Entry<String, String> entry : originalNames.entrySet()) {
            File renamedFile = new File(entry.getKey());
            if (renamedFile.exists()) {
                String originalName = entry.getValue();
                File originalFile = new File(targetDir, originalName);
                
                if (renamedFile.renameTo(originalFile)) {
                    restoredCount++;
                }
            }
        }
        
        Toast.makeText(this, 
                "成功还原 " + restoredCount + " 个文件", 
                Toast.LENGTH_SHORT).show();
        originalNames.clear();
    }
}