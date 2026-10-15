package mspaint.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * RecentFiles: the pictures opened or saved most recently, kept between runs.
 */
public class RecentFiles {
    private static final int MAX_FILES = 8;
    private static final String KEY = "recentFiles";
    private static final Preferences PREFS = Preferences.userRoot().node("mspaint");

    public static List<File> list() {
        List<File> files = new ArrayList<>();
        for(String path : PREFS.get(KEY, "").split("\n")) {
            if(!path.isEmpty() && new File(path).isFile()) {
                files.add(new File(path));
            }
        }
        return files;
    }

    public static void add(File file) {
        List<File> files = list();
        files.remove(file.getAbsoluteFile());
        files.add(0, file.getAbsoluteFile());
        StringBuilder value = new StringBuilder();
        for(int i = 0; i < files.size() && i < MAX_FILES; i++) {
            value.append(files.get(i).getPath()).append('\n');
        }
        PREFS.put(KEY, value.toString());
    }
}
