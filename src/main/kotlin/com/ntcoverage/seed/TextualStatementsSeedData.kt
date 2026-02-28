package com.ntcoverage.seed

data class TextualStatementSeedEntry(
    val fatherNormalizedName: String,
    val topic: String,
    val statementText: String,
    val originalLanguage: String? = null,
    val originalText: String? = null,
    val sourceWork: String,
    val sourceReference: String,
    val approximateYear: Int? = null
)

object TextualStatementsSeedData {

    val entries: List<TextualStatementSeedEntry> = listOf(

        // === IRENAEUS OF LYON ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "irenaeus_of_lyon",
            topic = "MANUSCRIPTS",
            statementText = "Matthew also issued a written Gospel among the Hebrews in their own dialect, while Peter and Paul were preaching at Rome and laying the foundations of the Church.",
            sourceWork = "Adversus Haereses",
            sourceReference = "3.1.1",
            approximateYear = 180
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "irenaeus_of_lyon",
            topic = "CANON",
            statementText = "It is not possible that the Gospels can be either more or fewer in number than they are. For since there are four zones of the world in which we live, and four principal winds, the Church is scattered throughout all the world, and the pillar and ground of the Church is the Gospel and the spirit of life; it is fitting that she should have four pillars.",
            sourceWork = "Adversus Haereses",
            sourceReference = "3.11.8",
            approximateYear = 180
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "irenaeus_of_lyon",
            topic = "TEXTUAL_VARIANTS",
            statementText = "The number 666 is found in all the most approved and ancient copies, and those men who saw John face to face bear their testimony to it.",
            sourceWork = "Adversus Haereses",
            sourceReference = "5.30.1",
            approximateYear = 180
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "irenaeus_of_lyon",
            topic = "CORRUPTION",
            statementText = "Marcion mutilated the Gospel according to Luke, removing all the prophecies about the Lord and much of the teaching of the Lord's discourses.",
            sourceWork = "Adversus Haereses",
            sourceReference = "1.27.2",
            approximateYear = 180
        ),

        // === ORIGEN ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "origen",
            topic = "TEXTUAL_VARIANTS",
            statementText = "The differences among the manuscripts have become great, either through the negligence of some copyists or through the perverse audacity of others; they either neglect to check over what they have transcribed, or, in the process of checking, they make additions or deletions as they please.",
            sourceWork = "Commentary on Matthew",
            sourceReference = "15.14",
            approximateYear = 248
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "origen",
            topic = "AUTOGRAPHS",
            statementText = "As I have learned by tradition respecting the four Gospels, which alone are uncontroverted in the Church of God under heaven: the first written was by Matthew, who was once a tax-collector but afterwards an apostle of Jesus Christ, published for the believers of Jewish origin, composed in the Hebrew language.",
            sourceWork = "Commentary on Matthew (ap. Eusebius, H.E.)",
            sourceReference = "6.25.4",
            approximateYear = 244
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "origen",
            topic = "CANON",
            statementText = "Paul did not write to all the churches which he had instructed; and even to those to which he wrote he sent but a few lines. And Peter, on whom the Church of Christ is built, has left one acknowledged epistle; possibly also a second, but this is doubtful.",
            sourceWork = "Commentary on Matthew (ap. Eusebius, H.E.)",
            sourceReference = "6.25.8",
            approximateYear = 244
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "origen",
            topic = "TEXTUAL_VARIANTS",
            statementText = "Today the fact is evident that there are many differences among the manuscripts, either due to the carelessness of scribes, or the perverse audacity of some people in correcting the text, or to the fact that there are those who add or delete as they see fit in making corrections.",
            sourceWork = "Contra Celsum",
            sourceReference = "2.27",
            approximateYear = 248
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "origen",
            topic = "SCRIPTURE_AUTHORITY",
            statementText = "The Scriptures were composed by the Spirit of God, and have a meaning, not such only as is apparent at first sight, but also another, which escapes the notice of most. For those words which are written are the forms of certain mysteries, and the images of divine things.",
            sourceWork = "De Principiis",
            sourceReference = "1.Preface.8",
            approximateYear = 230
        ),

        // === EUSEBIUS OF CAESAREA ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "eusebius_of_caesarea",
            topic = "MANUSCRIPTS",
            statementText = "Constantine commanded that fifty copies of the sacred Scriptures should be written on prepared parchment in a legible manner and in a convenient portable form, by professional transcribers thoroughly practiced in their art.",
            sourceWork = "Vita Constantini",
            sourceReference = "4.36",
            approximateYear = 337
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "eusebius_of_caesarea",
            topic = "CANON",
            statementText = "Among the accepted books must be placed the holy quaternion of the Gospels; these are followed by the Acts of the Apostles. After this must be reckoned the epistles of Paul. Following them the first epistle of John and likewise the first epistle of Peter must be maintained. After these, if it really seem proper, the Apocalypse of John.",
            sourceWork = "Historia Ecclesiastica",
            sourceReference = "3.25.1-4",
            approximateYear = 325
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "eusebius_of_caesarea",
            topic = "CANON",
            statementText = "Among the disputed writings, which are nevertheless recognized by many, are extant the so-called epistle of James and that of Jude, also the second epistle of Peter, and those that are called the second and third of John, whether they belong to the evangelist or to another person of the same name.",
            sourceWork = "Historia Ecclesiastica",
            sourceReference = "3.25.3",
            approximateYear = 325
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "eusebius_of_caesarea",
            topic = "AUTOGRAPHS",
            statementText = "Matthew had begun by preaching to the Hebrews, and when he was about to go to others too, he committed his own Gospel to writing in his native tongue and thus compensated those whom he was obliged to leave for the loss of his presence.",
            sourceWork = "Historia Ecclesiastica",
            sourceReference = "3.24.6",
            approximateYear = 325
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "eusebius_of_caesarea",
            topic = "TEXTUAL_VARIANTS",
            statementText = "The passage about the woman taken in adultery is not found in the most accurate copies, and is not acknowledged by the most esteemed ecclesiastical writers among the ancients.",
            sourceWork = "Historia Ecclesiastica",
            sourceReference = "3.39.17 (commentary tradition)",
            approximateYear = 325
        ),

        // === JEROME ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "jerome",
            topic = "TRANSLATION",
            statementText = "I am not so stupid as to think that any of the Lord's words either need correcting or are not divinely inspired; but the Latin manuscripts of the Scriptures are proved faulty by the variations which all of them exhibit, and my object has been to restore them to the form of the Greek original.",
            sourceWork = "Epistulae",
            sourceReference = "Ep. 27.1 (ad Marcellam)",
            approximateYear = 384
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "jerome",
            topic = "TRANSLATION",
            statementText = "You compel me to make a new work out of an old one, and after so many copies of the Scriptures have been scattered throughout the world, to sit in judgment and decide which of them agree with the Greek original.",
            sourceWork = "Praefatio in Quatuor Evangelia",
            sourceReference = "Preface to the Vulgate Gospels",
            approximateYear = 383
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "jerome",
            topic = "CORRUPTION",
            statementText = "The Latin texts are as numerous as the copies. Each person has added or changed what he thought fit, and there are nearly as many readings as manuscripts.",
            sourceWork = "Praefatio in Quatuor Evangelia",
            sourceReference = "Preface to the Vulgate Gospels",
            approximateYear = 383
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "jerome",
            topic = "MANUSCRIPTS",
            statementText = "I have used as the basis of my correction the old Greek codices. I have compared them with those which we call by the names of Lucian and Hesychius, and which the false scholarship of men has perverted.",
            sourceWork = "Praefatio in Quatuor Evangelia",
            sourceReference = "Preface to the Vulgate Gospels",
            approximateYear = 383
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "jerome",
            topic = "TEXTUAL_VARIANTS",
            statementText = "The passage about the woman taken in adultery, in the Gospel according to John, is found in many copies, both Greek and Latin.",
            sourceWork = "Adversus Pelagianos",
            sourceReference = "2.17",
            approximateYear = 415
        ),

        // === AUGUSTINE ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "augustine_of_hippo",
            topic = "SCRIPTURE_AUTHORITY",
            statementText = "I have learned to yield this respect and honor only to the canonical books of Scripture: of these alone do I most firmly believe that the authors were completely free from error. And if in these writings I am perplexed by anything which appears to me opposed to truth, I do not hesitate to suppose that either the manuscript is faulty, or the translator has not caught the meaning, or I myself have failed to understand.",
            sourceWork = "Epistulae",
            sourceReference = "Ep. 82.1.3 (ad Hieronymum)",
            approximateYear = 405
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "augustine_of_hippo",
            topic = "TRANSLATION",
            statementText = "Among translations themselves the Itala is to be preferred to the others, for it keeps closer to the words without prejudice to clearness of expression. And in correcting any Latin version, Greek ones should be consulted.",
            sourceWork = "De Doctrina Christiana",
            sourceReference = "2.15.22",
            approximateYear = 397
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "augustine_of_hippo",
            topic = "CORRUPTION",
            statementText = "When we find variations in the copies of the Holy Scriptures, or suspect a passage to be corrupt, we should have recourse to those manuscripts which are the most ancient and in the greatest agreement, and especially to those written in the original language.",
            sourceWork = "De Doctrina Christiana",
            sourceReference = "2.14.21",
            approximateYear = 397
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "augustine_of_hippo",
            topic = "CANON",
            statementText = "In the matter of canonical Scriptures, the authority of the majority of catholic Churches should be followed, among which are those that have deserved to have apostolic seats and to receive epistles.",
            sourceWork = "De Doctrina Christiana",
            sourceReference = "2.8.12",
            approximateYear = 397
        ),

        // === ATHANASIUS OF ALEXANDRIA ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "athanasius_of_alexandria",
            topic = "CANON",
            statementText = "These are the fountains of salvation, that they who thirst may be satisfied with the living words they contain. In these alone is proclaimed the doctrine of godliness. Let no man add to these, neither let him take aught from these.",
            sourceWork = "Epistula Festalis",
            sourceReference = "Festal Letter 39",
            approximateYear = 367
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "athanasius_of_alexandria",
            topic = "CANON",
            statementText = "Again, there are other books besides the foregoing, which are not indeed included in the Canon, but appointed by the Fathers to be read by those who newly join us, and who wish for instruction: the Wisdom of Solomon, and the Wisdom of Sirach, and Esther, and Judith, and Tobit, and the Didache, and the Shepherd.",
            sourceWork = "Epistula Festalis",
            sourceReference = "Festal Letter 39",
            approximateYear = 367
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "athanasius_of_alexandria",
            topic = "SCRIPTURE_AUTHORITY",
            statementText = "The holy and God-breathed Scriptures are self-sufficient for the preaching of the truth.",
            sourceWork = "Contra Gentes",
            sourceReference = "1.1",
            approximateYear = 335
        ),

        // === TERTULLIAN ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "tertullian",
            topic = "MANUSCRIPTS",
            statementText = "Come now, you who would indulge a better curiosity, if you would apply it to the business of your salvation, run over the apostolic churches, in which the very thrones of the apostles are still pre-eminent in their places, in which their own authentic writings are read.",
            sourceWork = "De Praescriptione Haereticorum",
            sourceReference = "36",
            approximateYear = 200
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "tertullian",
            topic = "APOCRYPHA",
            statementText = "I know that the writing which is called the Acts of Paul and Thecla, under the name of Paul, was forged by a presbyter of Asia. He was convicted and, having confessed that he had done it from love of Paul, was removed from his office.",
            sourceWork = "De Baptismo",
            sourceReference = "17",
            approximateYear = 200
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "tertullian",
            topic = "CORRUPTION",
            statementText = "Marcion expressly and openly used the knife, not the pen, since he made such an excision of the Scriptures as suited his own subject-matter.",
            sourceWork = "De Praescriptione Haereticorum",
            sourceReference = "38",
            approximateYear = 200
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "tertullian",
            topic = "CANON",
            statementText = "We lay it down as our first position, that the evangelical Testament has apostles for its authors, to whom was assigned by the Lord Himself this office of publishing the gospel.",
            sourceWork = "Adversus Marcionem",
            sourceReference = "4.2",
            approximateYear = 207
        ),

        // === CLEMENT OF ALEXANDRIA ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "clement_of_alexandria",
            topic = "AUTOGRAPHS",
            statementText = "The blessed presbyters, who preserved the true tradition of the blessed teaching directly from the holy apostles, Peter, James, John, and Paul, the sons receiving it from the father, came by God's will to us to deposit those ancestral and apostolic seeds.",
            sourceWork = "Stromata",
            sourceReference = "1.1.11",
            approximateYear = 200
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "clement_of_alexandria",
            topic = "MANUSCRIPTS",
            statementText = "Again, in the same books, Clement gives the tradition of the earliest presbyters, as to the order of the Gospels, in the following manner. The Gospels containing the genealogies, he says, were written first. But last of all John, perceiving that the bodily facts had been made plain in the Gospel, at the urging of his friends and inspired by the Spirit, composed a spiritual Gospel.",
            sourceWork = "Hypotyposeis (ap. Eusebius, H.E.)",
            sourceReference = "6.14.7",
            approximateYear = 200
        ),

        // === CYPRIAN OF CARTHAGE ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "cyprian_of_carthage",
            topic = "SCRIPTURE_AUTHORITY",
            statementText = "Whence is that tradition? Does it descend from the authority of the Lord and of the Gospel, or does it come from the commands and the epistles of the apostles? For that those things which are written must be done, God witnesses and warns.",
            sourceWork = "Epistulae",
            sourceReference = "Ep. 74.2",
            approximateYear = 256
        ),

        // === PAPIAS OF HIERAPOLIS ===

        TextualStatementSeedEntry(
            fatherNormalizedName = "papias_of_hierapolis",
            topic = "AUTOGRAPHS",
            statementText = "Mark, having become the interpreter of Peter, wrote down accurately, though not in order, whatsoever he remembered of the things said or done by Christ. For he neither heard the Lord nor accompanied Him, but later, as I said, he accompanied Peter.",
            sourceWork = "Exposition of the Sayings of the Lord (ap. Eusebius, H.E.)",
            sourceReference = "3.39.15",
            approximateYear = 110
        ),
        TextualStatementSeedEntry(
            fatherNormalizedName = "papias_of_hierapolis",
            topic = "AUTOGRAPHS",
            statementText = "Matthew put together the oracles in the Hebrew language, and each one interpreted them as best he could.",
            sourceWork = "Exposition of the Sayings of the Lord (ap. Eusebius, H.E.)",
            sourceReference = "3.39.16",
            approximateYear = 110
        )
    )
}
