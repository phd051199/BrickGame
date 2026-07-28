-dontoptimize
-target 1.1
-microedition

-keep public class e23.E23Midlet {
    public <init>();
    public void startApp();
    public void pauseApp();
    public void destroyApp(boolean);
}

-allowaccessmodification
-repackageclasses ''
-dontusemixedcaseclassnames
