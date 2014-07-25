/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.thunder;

public class SettingsController {

    static boolean clearHistory = false;

    /**
     * The purpose of this class is so that I can clear the dropdown history in
     * the main activities if the user selects to clear the history from the
     * disk in advanced settings
     * 
     * @param choice
     */
    public static void setClearHistory(boolean choice) {
	clearHistory = choice;
    }

    /**
     * return the choice
     * 
     * @return
     */
    public static boolean getClearHistory() {
	if (clearHistory) {
	    clearHistory = false;
	    return true;
	}
	return false;
    }
}
