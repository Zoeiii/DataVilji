package settings;

/**
 * This enumerable type lists the various application-specific property types listed in the initial set of properties to
 * be loaded from the workspace properties <code>xml</code> file specified by the initialization parameters.
 *
 * @author Ritwik Banerjee
 * @see vilij.settings.InitializationParams
 */
public enum AppPropertyTypes {

    /* resource files and folders */
    DATA_RESOURCE_PATH,
    CSS_RESOURCE_PATH,

    /* user interface icon file names */
    SCREENSHOT_ICON,
    SETTING_ICON,
    RUN_ICON,

    /* tooltips for user interface buttons */
    SCREENSHOT_TOOLTIP,

    /* warming messages*/
    EXIT_WHILE_RUNNING_WARNING,

    /* error messages */
    RESOURCE_SUBDIR_NOT_FOUND,
    LOAD_TOO_MANY,
    SHOW_10_LINE_ONLY,
    INVALID_INTERV,
    INVALID_ITER,
    NOT_INT,
    DISPLAY_LINE_ERROR,
    TOO_MANY_LABELS,

    /* application-specific message titles */
    SAVE_UNSAVED_WORK_TITLE,
    EXIT_WHILE_RUNNING,
    ALGO_RUN_CON_TITLE,
    MAX_ITERATION,
    UPDATE_INTERVAL,
    NUMBER_LABLES_TA,
    RUN_OR_NOT,
    RE_INPUT,

    /* application-specific messages */
    SAVE_UNSAVED_WORK,

    /* type-of algo names */
    CLASSIFICATION,
    CLUSTERING,
    ALGO_TYPES,
    R_CLASS,
    R_CLUST,
    K_CLUST,
    RandomClassifier,
    KMeansClusterer,
    RandomClusterer,

    /* application-specific parameters */
    IMAGE_FILE_EXT,
    IMAGE_FILE_EXT_DESC,
    DATA_FILE_EXT,
    DATA_FILE_EXT_DESC,
    TEXT_AREA,
    SPECIFIED_FILE,
    LEFT_PANE_TITLE,
    LEFT_PANE_TITLEFONT,
    LEFT_PANE_TITLESIZE,
    CHART_TITLE,
    DISPLAY_BUTTON_TEXT,
    CHECK_BOX_TEXT,
    NEW_LINE,
    X, Y,
    LOAD_FROM,
    NUMBER_INSTANCE,
    NUMBER_LABLES,
    ListOfLable,
    FORMAT_ERROR,
    FINISH,
    START_OVER_BUTTON
}
