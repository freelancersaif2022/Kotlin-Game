# PdfBox-Android: optional JPX/JBIG2 decoders and desktop AWT classes are not present on Android.
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.bind.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn org.slf4j.**

# Keep the model classes Room/Hilt generate against (already handled by their own consumer rules).
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
