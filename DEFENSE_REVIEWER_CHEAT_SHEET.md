# 🎓 TCA APP — COMPLETE DEFENSE & PITCH REVIEWER CHEAT SHEET
**Para sa Capstone / Thesis Defense & Project Pitching**
*(Bisaya + English Study Guide)*

---

## 📌 PART 1: MGA PANGUTANA BAHIN SA ALGORITHMS

### 1. Unsa ang Algorithm sa AI?
Sa atong backend (`functions/index.js`), tulo (3) ka algorithms ang naglangkob sa atong **AI & Smart Moderation Engine**:
1. **Perceptual Hashing (aHash - Computer Vision AI):**
   * *Giunsa paglihok:* Gikuha ang 8x8 pixel grayscale matrix sa gi-upload nga hulagway gamit ang Luma formula ($0.299R + 0.587G + 0.114B$) aron makahimo ug **64-bit visual fingerprint**. Bisan gi-crop o gi-resize, mailhan gihapon.
2. **Hamming Distance Algorithm (Similarity AI):**
   * *Giunsa paglihok:* Gitandi ang tagsa-tagsa ka bit sa duha ka image fingerprint aron makwenta ang `aiScore` (Pananglitan: `92% Match`). Kung lapas sa threshold, auto-delete o flag kay duplicate.
3. **Jaccard Similarity Index (NLP Text AI):**
   * *Giunsa paglihok:* Pormula sa Set Theory: $J(A, B) = \frac{|A \cap B|}{|A \cup B|}$. Gisukod niini ang pulong-sa-pulong nga kaparehasan sa duha ka post aron ma-detect ang text spam o duplicate posts.
4. **Heuristic Rule-Based Classifier (`AiCategoryClassifier.java`):**
   * *Giunsa paglihok:* Awtomatikong basahon ang keywords sa gi-type sa user ug ibutang dayon sa saktong kategorya (*Academics, Sports, Horoscopes, Events, Literature, etc.*).

---

### 2. Unsa ang mga Algorithm sa App (Non-AI / System)?
1. **Myers' Diff Algorithm (`DiffUtil` sa `HomeFeedFragment.java`):**
   * Gigamit para sa hapsay nga pag-render sa feed. Gina-compute ang minimum insertions/deletions aron walay lag o screen freeze inig naay bag-ong post.
2. **TimSort Algorithm (Java `Collections.sort`):**
   * Multi-level sorting: Unahon kanunay ang **Pinned Announcements** sa admin/officers, unya sundan sa **Chronological Order** (kinabag-ohang timestamp descending).
3. **Reed-Solomon Error Correction Algorithm (ZXing Engine):**
   * Ang mathematical algorithm sa luyo sa QR Code (`QRCodeWriter`). Makabasa gihapon bisan 15%–30% hugaw, garas, o hanap ang QR Code.
4. **Bounding Box Intersection Area Ratio (`VideoScrollHelper.java`):**
   * Geometric visibility check: Kung $\ge 50\%$ sa video ang Makita sa screen, mo-auto play; kung ubos sa 50%, mo-pause aron makadaginot ug data ug battery.

---

### 3. Nganong kana man inyong gigamit nga mga Algo?
* **Paspas ug Efficient ($O(1)$ to $O(N)$):** Dili makapa-init sa cellphone sa estudyante.
* **Low Server Cost:** Ang Perceptual Hashing ug Jaccard Index gaan kaayo daganon sa server (Serverless Firebase Cloud Functions) itandi sa bug-at kaayo nga mga deep learning models.
* **Fault-Tolerant:** Ang Reed-Solomon QR algorithm makaseguro nga makasulod ang attendance bisan daan o ubos ug camera quality ang phone sa estudyante.
* **Zero UI Lag:** Ang Myers' Diff algorithm nanalipod sa app gikan sa lag ug out-of-memory crashes.

---

### 4. Meanings ug Para Asa ang mga Algo?
| Algorithm | Meaning / Kahulugan | Para Asa / Function |
|---|---|---|
| **aHash (Average Hash)** | Paghimo ug 64-bit visual fingerprint base sa average brightness | Para mailhan ang kinopya o duplicate nga hulagway |
| **Hamming Distance** | Pagsukod sa gidaghanon sa nagkalahi nga bits sa duha ka binary strings | Para pagkwenta sa AI Match Percentage (`aiScore`) |
| **Jaccard Similarity** | Pagsukod sa intersection over union sa duha ka sets sa mga pulong | Para pag-detect sa duplicate text ug spam posts |
| **Regex Lexical Matching** | Pattern search gamit ang word boundaries (`\bword\b`) | Para automated filtering sa cyber libel ug law-ay nga pulong (RA 10175) |
| **Reed-Solomon** | Polynomial error-correcting code | Para sa QR barcode scanning ug error tolerance |
| **Myers' Diff** | Shortest Edit Script (SES) algorithm | Para smooth RecyclerView rendering nga dili mo-flicker |
| **TimSort** | Hybrid MergeSort + InsertionSort | Para han-ay ug paspas nga post sorting |

---

### 5. Nganong daghan man mo ug Algo?
* **Tubag:** *"Sir/Ma'am, kay ang matag feature sa mobile app naay lain-laing panginahanglan. Ang pag-sort sa post nagkinahanglan ug Sorting algorithm; ang pag-scan sa QR nagkinahanglan ug Error-correction algorithm; ang pag-detect sa kinopya nga hulagway nagkinahanglan ug Computer Vision hashing; ug ang pag-update sa screen nagkinahanglan ug Diffing algorithm. Walay usa ka algorithm nga makahimo sa tanan."*

---

## 📌 PART 2: DATA, STORAGE, & MEDIA MANAGEMENT

### 6. Unsaon pagbutang sa CSV file?
* **Pamaagi:** 
  1. I-save ang master list sa mga estudyante gikan sa Excel isip `.csv` (Comma-Separated Values).
  2. Gamit ang usa ka **Admin Upload Script (Node.js/Python)** o direkta pinaagi sa **Firebase Console / Firestore Extension**, ang CSV file basahon linya por linya (parser), i-convert ngadto sa JSON objects, ug i-save sa Firestore collection nga `official_students`.
  3. Ang mga columns sa CSV mao ang: `studentIdNumber, name, email, department, course, yearLevel`.

---

### 7. Unsa ang Database nga inyong gigamit?
* **Tubag:** **Google Cloud Firestore (NoSQL Document-Based Realtime Database)**.
* **Ngano NoSQL Firestore:** 
  * Realtime listeners (`addSnapshotListener`) — makakita dayon ang mga estudyante sa bag-ong posts, chats, ug updates nga walay manual refresh.
  * Auto-scaling — makadumala ug liboan ka concurrent users nga walay downtime.
  * Offline persistence — ma-cache ang data bisan mag-hinay ang signal.

---

### 8. Unsa ang gigamit aron maka-store mo ug Picture ug Video?
* **Tubag:** **Cloudinary Media CDN & Storage**.

---

### 9. Unsa ang gigamit para sa Barcode / QR Code?
* **Tubag:** **ZXing ("Zebra Crossing") Android Library** (`com.google.zxing`).
  * `QRCodeWriter` para mag-generate ug QR codes sa attendance ug polls.
  * CameraX / ZXing Scanner para magbasa sa QR codes pinaagi sa cellphone camera.

---

### 10. Nganong Cloudinary man inyong gigamit para sa Video ug Picture? (Nganong dili Firebase Storage?)
* **Libre ug Dako ug Bandwidth:** Ang Firebase Storage dali ra mahurot ang 1GB/day free quota inig daghan na ang mag-tan-aw ug videos. Ang Cloudinary naghatag ug libreng 25GB monthly credit.
* **On-the-fly Media Optimization:** Awtomatikong gi-compress ug gi-resize sa Cloudinary ang mga dagkong hulagway ug video (`f_auto,q_auto`) aron paspas kaayo mo-load sa hinay nga data/WiFi.
* **Global Content Delivery Network (CDN):** Duol sa users ang servers busa instant ang playback sa video feeds.

---

### 11. What does CSV file mean?
* **Meaning:** **CSV** stands for **Comma-Separated Values**.
* Kini usa ka plain text file format nga nagtipig sa tabular data (sama sa Excel rows ug columns). Ang matag linya mao ang usa ka record, ug ang matag column gibulag pinaagi sa comma (`,`).

---

## 📌 PART 3: CODE LOCATIONS & ARCHITECTURE

### 12. Asa dapit ang Code sa Firebase?
1. **Firestore Data Repository:** [`app/src/main/java/com/example/tca_app/PostRepository.java`](file:///c:/MOBILEAPP/TCA_APP/app/src/main/java/com/example/tca_app/PostRepository.java)
2. **Cloud Functions (AI Backend):** [`functions/index.js`](file:///c:/MOBILEAPP/TCA_APP/functions/index.js)
3. **Security Rules:** [`firestore.rules`](file:///c:/MOBILEAPP/TCA_APP/firestore.rules)
4. **Configuration / Credentials:** [`app/google-services.json`](file:///c:/MOBILEAPP/TCA_APP/app/google-services.json)

---

### 13. Asa dapit makita ang Code sa Cloudinary?
* **File:** [`app/src/main/java/com/example/tca_app/CloudinaryUploader.java`](file:///c:/MOBILEAPP/TCA_APP/app/src/main/java/com/example/tca_app/CloudinaryUploader.java)
  * Makita dinhi ang background multithreading (`uploadExecutor`), multipart/form-data HTTP POST request, ug secure direct-to-cloud upload.

---

### 14. Asa dapit makita ang Code sa HomeFeedFragment?
* **File:** [`app/src/main/java/com/example/tca_app/HomeFeedFragment.java`](file:///c:/MOBILEAPP/TCA_APP/app/src/main/java/com/example/tca_app/HomeFeedFragment.java)
* **XML Layout:** [`app/src/main/res/layout/fragment_home_feed.xml`](file:///c:/MOBILEAPP/TCA_APP/app/src/main/res/layout/fragment_home_feed.xml)

---

### 15. Asa dapit makita ang MAIN JD sa inyong pag-code?
* **Core Logic (Java Source Code):**
  * Folder: `app/src/main/java/com/example/tca_app/`
* **Root Activity nga nag-host sa tanang tabs:**
  * [`app/src/main/java/com/example/tca_app/MainActivity.java`](file:///c:/MOBILEAPP/TCA_APP/app/src/main/java/com/example/tca_app/MainActivity.java)
* **Launcher Entry (Unang moabli):**
  * [`app/src/main/java/com/example/tca_app/WelcomeActivity.java`](file:///c:/MOBILEAPP/TCA_APP/app/src/main/java/com/example/tca_app/WelcomeActivity.java)

---

### 16. Giunsa ninyo pag-design? Nag-Drag and Drop mo?
* **Tubag:** **DILI, puro kini hand-coded XML gamit ang modern Android Design Systems**.
* **Nganong dili Drag & Drop:**
  * Ang drag-and-drop sa layout editor makamugna ug gubot, hardcoded pixel values (`px`/`dp`) nga magkandagisal ug madaot sa lain-laing screen sizes.
  * Ang hand-coded XML naggamit ug responsive layouts: `ConstraintLayout`, `LinearLayout`, `RelativeLayout`, styles sa `values/themes.xml`, ug reusable drawables sa `res/drawable/` aron perpekto ang itsa sa bisan unsang gidak-on sa phone.

---

### 17. Unsa nga Activity ang inyong gigamit?
* Gigamit namo ang **Single-Host Activity with Multiple Navigation Fragments** pattern (rekomendado sa Google Android Architecture):
  1. `MainActivity.java` — Mao ang main container nga nagpadagan sa Bottom Navigation Bar.
  2. `HomeFeedFragment.java` — News Feed tab.
  3. `EventCalendarFragment.java` — Calendar, Events, Attendance QR, ug Voting Polls tab.
  4. `OrganizationFragment.java` — Editorial Chart ug Campus Organizations tab.
  5. `AdminDashboardFragment.java` — Content moderation queue ug analytics.
  6. `ProfileFragment.java` — User profile, saved posts, ug dark mode settings.
  * Uban pang dedicated Activities: `LoginActivity.java`, `CreatePostActivity.java`, `QRScannerActivity.java`, `MessageActivity.java`, `DarkModeActivity.java`.

---

### 18. Giunsa ninyo pag-connect sa Firebase?
1. **Google Services Plugin:** Gi-apply ang `com.google.gms.google-services` plugin sa `build.gradle`.
2. **Project Credentials:** Gibutang ang opisyal nga configuration file nga [`google-services.json`](file:///c:/MOBILEAPP/TCA_APP/app/google-services.json) sa sulod sa `app/` folder.
3. **Firebase SDK Initializer:** Sa pag-abli sa app, ang Google Services Provider awtomatikong magbasa sa `google-services.json` (Project ID: `tca-app-e3ce4`, App ID, API Keys) aron i-initialize ang `FirebaseAuth.getInstance()` ug `FirebaseFirestore.getInstance()`.

---

## 🌟 BONUS: MGA PANGUTANA NGA POSIBLE IPANGUTANA SA PANEL (PITCH / DEFENSE)

### Q1: Unsa ang Security Features sa inyong App batok sa Hackers o Unauthorized Voters?
* **Tubag:**
  1. **Role-Based Access Control (RBAC):** Pinaagi sa `firestore.rules`, ang mga admins ug authorized editorial board ra ang makamugna ug official announcements ug voting polls.
  2. **One-Vote-Per-Student Lock:** Sa voting algorithm, ang matag boto nagtipig sa UID sa estudyante sulod sa `voterUids` array. Kung niboto na ang UID, i-block dayon kini sa app ug backend.
  3. **Direct Server Timestamping:** Gigamit ang `FieldValue.serverTimestamp()` aron dili madaya o mausab ang oras sa attendance ug boto bisan usbon sa estudyante ang oras sa iyang cellphone.

### Q2: Unsay nakapalahi sa inyong App sa ordinaryong Facebook Group o Google Forms?
* **Tubag:**
  1. **All-in-One Campus Eco-system:** Gi-hiusa ang Opisyal nga Balita, Realtime Chat, Event Calendar, QR Attendance, ug Anti-Cheat Voting Polls sa usa ka aplikasyon.
  2. **Automated AI Moderation:** Naay built-in nga proteksyon batok sa Cyber Libel (RA 10175) ug duplicate media spam nga walay manual human effort.
  3. **Privacy & Exclusivity:** Mga opisyal nga estudyante ug kawani ra sa BISU ang makasulod base sa `official_students` database.

### Q3: Unsa ang Architecture sa inyong Application?
* **Tubag:** Naggamit kami og **MVVM (Model-View-ViewModel) with Repository Pattern**. Dili magsagol ang UI design ug ang database code, aron hapsay, sayon i-maintain, ug production-ready.

### Q4: Unsay mahitabo kung kalit maputol ang Internet / Data sa estudyante?
* **Tubag:** Mo-gana gihapon ang app tungod sa **Firestore Offline Persistence (Local Caching)**. Makabasa gihapon ang estudyante sa mga post, profile, ug charts nga na-cache na sa iyang cellphone.

### Q5: Giunsa ninyo pagsiguro nga tinuod nga BISU student ang mag-register ug dili taga-gawas?
* **Tubag:** Pinaagi sa **Pre-registration Verification**. Sa registration screen, i-cross-reference sa sistema ang Student ID Number ug Email batok sa opisyal nga `official_students` masterlist sa Firestore bago tugotan nga makahimo og account.

### Q6: Nganong QR Code man ang gigamit sa Attendance ug Voting, nganong dili na lang button sa screen?
* **Tubag:** Aron masiguro ang **Physical Presence** sa estudyante sa event venue ug malikayan ang pag-proxy o pag-attend sa attendance samtang anaa ra sa balay.

### Q7: Unsay buhaton kung naay makalusot nga sayop nga balita o bastos nga post nga wala na-block sa AI?
* **Tubag:** Naay **"Report Post" feature** ang mga estudyante. Sa pag-report, awtomatiko kining moadto sa **Admin Moderation Queue** aron ma-review ug ma-delete dayon sa Admin.

### Q8: Nganong Java man ang gigamit ninyo nga language ug dili Kotlin o Flutter?
* **Tubag:** Kay lig-on ang Java sa enterprise native Android, mas gamay og compatibility issues sa mga daan ug bag-ong Android versions, ug standard sa Google Services ug ZXing camera integrations.

### Q9: Kung 5,000 ka estudyante ang magdungan og gamit sa Intramurals, dili ba mo-crash inyong database?
* **Tubag:** Dili, tungod kay **Serverless ug Auto-scaling ang Google Cloud Firestore** nga maka-cater og minilyon ka concurrent operations, ug naggamit mi og **Pagination (50 posts per batch)** aron dili mabug-atan ang network.

### Q10: Unsaon ninyo pag-protect sa User Passwords batok sa mga Hacker?
* **Tubag:** Ang mga password gidumala sa **Firebase Authentication** gamit ang salted cryptographic hash algorithm (PBKDF2/SHA-256). Bisan ang mga developers o admins dili makakita sa actual password sa user.

### Q11: Kinsay gitugotan nga makakita o maka-edit sa Editorial Member Chart?
* **Tubag:** Ang ordinaryong mga estudyante **igo ra makatan-aw (Read-Only / View-Only)**. Ang mga verified Admins ug Organization Officers ra ang naay katungod nga mag-edit o mag-assign og posisyon.

### Q12: Nganong gigamit ninyo ang Cloudinary imbes nga i-save diretso ang pictures sulod sa database (Firestore)?
* **Tubag:** Kay ang Firestore gidisenyo para sa **Structured Text Data** lamang (naay 1MB limit per document). Ang mga hulagway ug videos kinahanglan sa usa ka **Dedicated Media CDN sama sa Cloudinary** aron paspas ug dili ma-corrupt ang database.
