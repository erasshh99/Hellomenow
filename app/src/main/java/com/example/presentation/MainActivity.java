package com.example.presentation;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.R;
import com.example.presentation.ui.ChatFragment;
import com.example.presentation.ui.ConversationsFragment;
import com.example.presentation.ui.MediaGalleryFragment;
import com.example.presentation.ui.ModelsFragment;
import com.example.presentation.ui.ProvidersFragment;
import com.example.presentation.ui.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            return windowInsets;
        });

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                switchFragment(ChatFragment.newInstance("welcome_chat"));
                return true;
            } else if (itemId == R.id.nav_providers) {
                switchFragment(new ProvidersFragment());
                return true;
            } else if (itemId == R.id.nav_models) {
                switchFragment(new ModelsFragment());
                return true;
            } else if (itemId == R.id.nav_gallery) {
                switchFragment(new MediaGalleryFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            switchFragment(ChatFragment.newInstance("welcome_chat"));
        }
    }

    public void navigateToChat(String conversationId) {
        bottomNav.setSelectedItemId(R.id.nav_chat);
        switchFragment(ChatFragment.newInstance(conversationId));
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
