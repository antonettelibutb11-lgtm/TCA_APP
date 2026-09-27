# 🎓 TCA APP — ALL DEFENSE QUESTIONS & ANSWERS (OFFICIAL STUDY GUIDE)
**Pinaka-Simple, Direct to the Point, ug Dali Masag-ulo**
*(Study Guide para sa Capstone / Thesis Defense & Project Pitching)*

---

## 📋 PART 1: ANG IMONG 18 KA MGA PANGUTANA

### 1. UNSA ANG ALGORITHM SA AI?
* **Tubag:** **Perceptual Hashing (aHash)** para sa hulagway, **Jaccard Similarity** para sa text, ug **Regex Word Boundary Matching** para sa cyber libel detection.

### 2. UNSA ANG ALGO SA APP (Non-AI / System)?
* **Tubag:** **Myers' Diff Algorithm** (para walay lag ang feed), **TimSort** (para sa pag-sort sa posts), ug **Reed-Solomon** (para sa QR code).

### 3. NGANONG KANA MAN INYONG GIGAMIT NGA ALGO?
* **Tubag:** Kay **paspas sila, gaan sa cellphone, libre**, ug dili makapa-init o makapa-hang sa app.

### 4. TAGA-I KOG MGA MEANING ANANG MGA ALGO UG PARA ASA SILA?
* **aHash (Average Hash):** Visual fingerprint $\rightarrow$ *Para ilhon ang kinopya o duplicate nga hulagway.*
* **Hamming Distance:** Comparison math $\rightarrow$ *Para pagkwenta sa AI Match Score (%).*
* **Jaccard Similarity:** Word overlap checker $\rightarrow$ *Para ilhon ang duplicate o spam nga text post.*
* **Regex:** Word boundary filter $\rightarrow$ *Para i-block ang malaw-ay ug cyber libel nga mga pulong (RA 10175).*
* **Reed-Solomon:** Error correction $\rightarrow$ *Para mabasa gihapon ang QR Code bisan hanap o hugaw ang camera.*
* **Myers' Diff:** Shortest Edit Script (SES) $\rightarrow$ *Para hapsay ug smooth nga list updates sa RecyclerView.*
* **TimSort:** Hybrid Sorting algorithm $\rightarrow$ *Para unahon ang Pinned Announcements unya ang bag-ong posts.*

### 5. NGANONG DAGHAN MAN MO UG ALGO?
* **Tubag:** Kay **lahi-lahi ang trabaho sa matag feature** — lahi ang algo sa pag-sort, lahi sa QR code, ug lahi pud sa pag-detect og kinopya nga hulagway. Walay usa ka algorithm nga makahimo sa tanan.

### 6. UNSAON MAN NINYO PAGBUTANG SA CSV FILE?
* **Tubag:** I-save ang masterlist sa estudyante gikan sa Excel isip **`.csv`**, unya i-upload kini ngadto sa Firestore database pinaagi sa **Firebase Console** o admin upload script.

### 7. UNSA ANG DATABASE NGA INYONG GIGAMIT?
* **Tubag:** **Google Cloud Firestore** — usa ka NoSQL Realtime Document-based Database.

### 8. UNSA ANG GIGAMIT ARON MAKA-STORE MO OG PIC UG VID?
* **Tubag:** **Cloudinary Media Storage & CDN**.

### 9. UNSA ANG GIGAMIT NINYO PARA SA BARCODE / QR?
* **Tubag:** **ZXing ("Zebra Crossing") Android Library** (`com.google.zxing`).

### 10. NGANONG CLOUDINARY INYONG GIGAMIT PARA SA VID UG PIC?
* **Tubag:** Kay **libre ug dako og bandwidth (25GB)**, ug awtomatiko niining i-compress ang pictures ug videos aron paspas mo-load bisan hinay ang data sa estudyante.

### 11. WHAT DOES CSV FILE MEAN?
* **Tubag:** Ang CSV nagpasabot og **Comma-Separated Values** — usa ka plain-text file format nga nagtipig og tabular data (sama sa Excel) diin ang kada column gibulag og comma (`,`).

### 12. ASA DAPIT ANG CODE SA FIREBASE?
* **Tubag:** Naa sa `PostRepository.java` (database queries), `functions/index.js` (cloud backend), ug `firestore.rules` (security).

### 13. ASA DAPIT MAKITA ANG CODE SA CLOUDINARY?
* **Tubag:** Naa sa `app/src/main/java/com/example/tca_app/CloudinaryUploader.java`.

### 14. ASA DAPIT MAKITA ANG CODE SA HOMEFEEDFRAGMENT?
* **Tubag:** Naa sa `HomeFeedFragment.java` (Java logic) ug `res/layout/fragment_home_feed.xml` (design layout).

### 15. ASA DAPIT MAKITA ANG MAIN GYUD SA PAG-CODE NINYO?
* **Tubag:** Naa sa folder nga `app/src/main/java/com/example/tca_app/`, ug ang pinaka-main host screen mao ang `MainActivity.java`.

### 16. GIUNSA NINYO PAG-DESIGN? NAG-DRAG AND DROP MO?
* **Tubag:** **Wala nag-drag and drop; puro kini Hand-Coded XML** gamit ang responsive layouts (`ConstraintLayout`, `LinearLayout`) aron dili maguba ang hitsura sa lain-laing cellphone.

### 17. UNSA NGA ACTIVITY ANG INYONG GIGAMIT?
* **Tubag:** Naggamit kami og **Single-Host Activity (`MainActivity`) nga may Navigation Fragments** (Home, Events, Orgs, Admin, Profile).

### 18. GIUNSA NINYO PAG-CONNECT SA FIREBASE?
* **Tubag:** Pinaagi sa pagbutang sa `google-services.json` configuration credentials file sa sulod sa among Android `app/` folder.

---

## 🌟 PART 2: DUGANG 12 KA PABORITONG PANGUTANA SA MGA PANELISTS

### Q19: Unsa ang Architecture sa inyong Application?
* **Tubag:** Naggamit kami og **MVVM (Model-View-ViewModel)** with Repository Pattern aron dili magsagol ang UI design ug database code, para hapsay ug production-ready.

### Q20: Unsay mahitabo kung kalit maputol ang Internet / Data sa estudyante? Mo-gana ba gihapon ang app?
* **Tubag:** Mo-gana gihapon tungod sa **Firestore Offline Persistence (Local Caching)** — makabasa gihapon siya sa mga post, charts, ug profile nga na-load na daan sa iyang cellphone.

### Q21: Giunsa ninyo pagsiguro nga tinuod nga BISU student ang mag-register ug dili taga-gawas?
* **Tubag:** Pinaagi sa **Pre-registration Verification**. I-cross-reference sa sistema ang Student ID Number ug Email batok sa opisyal nga masterlist (`official_students`) bago tugotan nga makahimo og account.

### Q22: Nganong QR Code man ang gigamit sa Attendance ug Voting, nganong dili na lang button sa screen?
* **Tubag:** Aron masiguro ang **Physical Presence** sa estudyante sa event venue ug malikayan ang pag-proxy o pag-attend samtang anaa ra sa balay.

### Q23: Giunsa ninyo paglikay nga dili kaduha makaboto ang estudyante sa voting poll?
* **Tubag:** Gi-record namo ang iyang Unique Student UID sulod sa `voterUids` list sa database; kung nakaboto na siya kaisa, i-block na dayon siya sa sistema.

### Q24: Nganong mas maayo man ni kaysa sa ordinaryong Facebook Group o Google Forms?
* **Tubag:** Kay kini usa ka **All-in-One Campus Eco-system** nga exclusive sa BISU students, naay automated AI moderation batok sa Cyber Libel (RA 10175), ug naay official QR attendance ug voting system.

### Q25: Unsay buhaton kung naay makalusot nga sayop nga balita o bastos nga post nga wala na-block sa AI?
* **Tubag:** Naay **Report Post button** ang mga estudyante diin awtomatiko kining moadto sa **Admin Moderation Queue** aron ma-delete dayon sa Admin.

### Q26: Nganong Java man ang inyong gigamit ug dili Kotlin o Flutter?
* **Tubag:** Kay lig-on ang Native Java sa enterprise Android, gamay og compatibility issues sa mga daan ug bag-ong phone models, ug dali i-integrate sa Google Firebase ug ZXing Camera SDK.

### Q27: Kung 5,000 ka estudyante ang magdungan og gamit sa Intramurals, dili ba mo-crash inyong database?
* **Tubag:** Dili, tungod kay **Auto-scaling ug Serverless ang Google Cloud Firestore**, ug naggamit mi og **Pagination (50 posts per batch)** aron dili mabug-atan ang network.

### Q28: Unsaon ninyo pag-protect sa User Passwords batok sa mga Hacker?
* **Tubag:** Ang mga password gidumala sa **Firebase Authentication** gamit ang salted cryptographic hash algorithm (PBKDF2/SHA-256) — bisan kaming mga developers ug admins dili makakita sa actual password sa user.

### Q29: Kinsay gitugotan nga makakita o maka-edit sa Editorial Member Chart?
* **Tubag:** Ang ordinaryong mga estudyante **igo ra makatan-aw (View-Only)**. Ang mga verified Admins ug Organization Officers ra ang naay katungod nga mag-edit o mag-assign og posisyon.

### Q30: Nganong gigamit ninyo ang Cloudinary imbes nga i-save diretso ang pictures sulod sa database (Firestore)?
* **Tubag:** Kay ang Firestore gidisenyo para sa **Structured Text Data** lamang (naay 1MB limit per document). Ang mga hulagway ug videos kinahanglan sa usa ka **Dedicated Media CDN sama sa Cloudinary** aron paspas mo-stream ug dili ma-corrupt ang database.

---

## 🛠️ PART 3: MGA PABORITONG PANGUTANA BAHIN SA ANDROID STUDIO

### Q31: Unsa ang Gradle ug para asa ang `build.gradle`?
* **Tubag:** Ang Gradle mao ang **Build Automation Tool**. Sa `build.gradle` gibutang ang tanang external libraries (Firebase, Cloudinary, ZXing) ug SDK versions.

### Q32: Unsa ang gamit sa `AndroidManifest.xml`?
* **Tubag:** Mao kini ang **Blueprint sa tibuok app**. Dinhi gi-declare ang permissions (Camera, Internet) ug ang tanang screens o Activities.

### Q33: Unsa ang Min SDK ug Target SDK sa inyong app?
* **Tubag:** Ang **Min SDK kay 24** (Android 7.0 pataas makadagan) ug ang **Target SDK kay 37** (naka-optimize sa pinakabag-ong Android versions).

### Q34: Nganong `RecyclerView` inyong gigamit sa feed ug dili `ListView`?
* **Tubag:** Kay ang `RecyclerView` nag-recycle sa mga views nga mawala sa screen gamit ang **ViewHolder Pattern** — mas paspas, tipid sa RAM, ug walay lag.

### Q35: Unsa ang kalainan sa Activity ug Fragment sa Android Studio?
* **Tubag:** Ang **Activity** mao ang tibuok bintana/screen (`MainActivity`), samtang ang **Fragment** mao ang sub-screen o modular tab sa sulod niini (Home, Events, Profile).

### Q36: Asa dapit gitipigan ang mga Colors, Text Labels, ug Layouts?
* **Tubag:** Sa folder nga **`res/`**: `res/layout` (XML designs), `res/values/colors.xml` (colors), ug `res/values/strings.xml` (labels).

### Q37: Giunsa ninyo pag-test ang app sa Android Studio? Emulator ba o Tinuod nga Phone?
* **Tubag:** Sa **Physical Device (Tinuod nga Cellphone)** pinaagi sa USB Debugging, kay kinahanglan og tinuod nga camera para sa QR scanning ug video testing.

### Q38: Unsa ang `Intent` sa Android?
* **Tubag:** Mao kini ang **Messaging Object** nga gigamit pag-balhin gikan sa usa ka screen paingon sa lain (pananglitan: Login $\rightarrow$ Home Feed).

### Q39: Unsa ang Android Activity Lifecycle?
* **Tubag:** Mao kini ang mga states sa screen: `onCreate` (paghimo), `onStart` (pagpakita), `onResume` (pag-interact), `onPause` (pag-background), ug `onDestroy` (pag-close).

### Q40: Unsaon ninyo pag-release sa app aron ma-install sa cellphone sa mga estudyante?
* **Tubag:** Mo-adto sa Android Studio: **Build $\rightarrow$ Generate Signed Bundle / APK** aron makahimo og standalone installable `.apk` file.

