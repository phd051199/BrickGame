# Convert modern javac output to CLDC-compatible Java 1.1 class files.
-dontoptimize
-dontobfuscate
-target 1.1
-microedition

-keep public class brickgame.Midlet {
    public <init>();
    public void startApp();
    public void pauseApp();
    public void destroyApp(boolean);
}

-keepattributes Exceptions,InnerClasses
