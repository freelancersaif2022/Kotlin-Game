package com.lexora.english.content

import java.time.LocalDate
import kotlin.random.Random

data class FillBlank(val before: String, val after: String, val answers: List<String>, val hint: String)
data class BuildSentence(val sentence: String, val meaning: String)
data class WordCard(val word: String, val type: String, val meaning: String, val example: String)
data class WritingPrompt(val title: String, val prompt: String, val minWords: Int, val keywords: List<String>)

/** Extra general questions used for the daily challenge and grammar tests. */
internal object QuestionBank {
    val general = listOf(
        q("Choose the correct spelling.", listOf("recieve", "receive", "receeve", "riceive"), 1, "i before e, except after c: receive."),
        q("Choose the correct spelling.", listOf("definately", "definitely", "definitly", "defanitely"), 1, "definitely — think of \"finite\"."),
        q("Choose the correct spelling.", listOf("accommodation", "acommodation", "accomodation", "acomodation"), 0, "Double c and double m."),
        q("Choose the correct spelling.", listOf("neccessary", "necessary", "necesary", "nesessary"), 1, "One c, two s: necessary."),
        q("Choose the correct spelling.", listOf("tommorow", "tomorow", "tomorrow", "tommorrow"), 2, "One m, two r: tomorrow."),
        q("What is the opposite of \"generous\"?", listOf("kind", "selfish", "honest", "brave"), 1, "selfish is the opposite of generous."),
        q("What is the opposite of \"ancient\"?", listOf("old", "modern", "huge", "rare"), 1, "modern is the opposite of ancient."),
        q("What is a synonym of \"huge\"?", listOf("tiny", "enormous", "narrow", "light"), 1, "enormous = very big."),
        q("What is a synonym of \"begin\"?", listOf("finish", "start", "stop", "leave"), 1, "begin = start."),
        q("What is a synonym of \"quick\"?", listOf("slow", "fast", "late", "calm"), 1, "quick = fast."),
        q("\"Hit the books\" means…", listOf("throw books", "study hard", "buy books", "close books"), 1, "An idiom meaning study hard."),
        q("\"Call it a day\" means…", listOf("phone someone", "stop working", "start early", "name the day"), 1, "Stop working for the day."),
        q("\"Once in a blue moon\" means…", listOf("very often", "every month", "very rarely", "at night"), 2, "Very rarely."),
        q("\"Cost an arm and a leg\" means…", listOf("be very expensive", "hurt", "be free", "be cheap"), 0, "Very expensive."),
        q("Complete: \"She ___ to the gym every morning.\"", listOf("go", "goes", "going", "gone"), 1, "he/she/it + -s."),
        q("Complete: \"I ___ my keys. I can't find them.\"", listOf("lose", "have lost", "am losing", "loses"), 1, "Present perfect: result now."),
        q("Complete: \"They ___ TV when I arrived.\"", listOf("watched", "were watching", "watch", "are watching"), 1, "Past continuous for background action."),
        q("Complete: \"If I ___ rich, I would buy a boat.\"", listOf("am", "were", "will be", "be"), 1, "Second conditional: if + past."),
        q("Complete: \"This is the ___ day of my life!\"", listOf("good", "better", "best", "most good"), 2, "Superlative: the best."),
        q("Complete: \"He is ___ engineer.\"", listOf("a", "an", "the", "—"), 1, "engineer starts with a vowel sound."),
        q("Complete: \"I'm good ___ maths.\"", listOf("in", "on", "at", "for"), 2, "good at."),
        q("Complete: \"We met ___ 2019.\"", listOf("on", "at", "in", "by"), 2, "in + year."),
        q("Complete: \"There isn't ___ milk left.\"", listOf("some", "any", "many", "a"), 1, "any in negatives."),
        q("Complete: \"How ___ people came?\"", listOf("much", "many", "lot", "more"), 1, "many + countable."),
        q("Complete: \"Could you tell me where the station ___?\"", listOf("is", "is it", "does", "are"), 0, "Indirect question: statement order."),
        q("Complete: \"You're coming tomorrow, ___?\"", listOf("are you", "aren't you", "don't you", "won't you"), 1, "Positive statement → negative tag."),
        q("Complete: \"The letter ___ yesterday.\"", listOf("sent", "was sent", "is sent", "sends"), 1, "Past passive: was sent."),
        q("Complete: \"I've lived here ___ ten years.\"", listOf("since", "for", "during", "ago"), 1, "for + period of time."),
        q("Complete: \"She speaks English very ___.\"", listOf("good", "well", "nice", "better"), 1, "Adverb: well."),
        q("Which sentence is correct?", listOf("I am agree.", "I agree.", "I agreeing.", "I do agree with."), 1, "agree is a verb: I agree."),
        q("Which sentence is correct?", listOf("He don't like it.", "He doesn't like it.", "He not like it.", "He doesn't likes it."), 1, "doesn't + base verb."),
        q("Which sentence is correct?", listOf("I have 25 years.", "I am 25 years old.", "I have 25 years old.", "I am 25 years."), 1, "Use be for age."),
        q("Which sentence is correct?", listOf("Explain me the rule.", "Explain the rule to me.", "Explain to me rule.", "Explain me rule."), 1, "explain something to someone."),
        q("Which sentence is correct?", listOf("I'm boring in class.", "I'm bored in class.", "I bored in class.", "I'm bore in class."), 1, "-ed for feelings."),
        q("Which sentence is correct?", listOf("She said me hello.", "She told me hello.", "She said hello to me.", "She told hello me."), 2, "say something to someone."),
        q("What does \"postpone\" mean?", listOf("cancel", "move to a later time", "start early", "repeat"), 1, "postpone = delay to later."),
        q("What does \"reliable\" mean?", listOf("can be trusted", "very fast", "expensive", "noisy"), 0, "reliable = trustworthy."),
        q("What does \"efficient\" mean?", listOf("wasteful", "working well without waste", "very slow", "beautiful"), 1, "Doing things well without wasting time or energy."),
        q("What does \"curious\" mean?", listOf("angry", "wanting to learn or know", "tired", "lazy"), 1, "curious = wanting to know."),
        q("What does \"deadline\" mean?", listOf("a dead person", "the last time to finish something", "a line on a road", "a holiday"), 1, "The latest time to finish."),
    )
}

object DailyQuiz {
    const val SIZE = 5

    private val pool: List<Question> by lazy {
        QuestionBank.general + Course.allQuestions() + Projects.allQuestions()
    }

    fun questionsFor(date: LocalDate): List<Question> {
        val rnd = Random(date.toEpochDay() * 7919 + 17)
        return pool.shuffled(rnd).take(SIZE)
    }

    private val topics = listOf(
        "grammar, vocabulary and idioms", "spelling and everyday phrases", "tenses and prepositions",
        "real-life situations", "word meanings and synonyms", "questions and conversation", "writing and punctuation",
    )

    fun topicLine(date: LocalDate): String = topics[(date.toEpochDay() % topics.size).toInt()]
}

object PracticeContent {

    fun testQuestions(count: Int = 10): List<Question> =
        (QuestionBank.general + Course.allQuestions()).shuffled().take(count)

    val fillBlanks = listOf(
        FillBlank("She", "to work by bus every day.", listOf("goes"), "go → present simple, she"),
        FillBlank("I have never", "to Japan.", listOf("been"), "be → past participle"),
        FillBlank("They", "watching a film when I called.", listOf("were"), "past continuous, they"),
        FillBlank("If it rains, we", "stay at home.", listOf("will", "'ll"), "first conditional"),
        FillBlank("This is", "best coffee in town.", listOf("the"), "article before a superlative"),
        FillBlank("He is interested", "photography.", listOf("in"), "interested + preposition"),
        FillBlank("My birthday is", "the 5th of May.", listOf("on"), "preposition for dates"),
        FillBlank("Could you tell me where the bank", "?", listOf("is"), "indirect question"),
        FillBlank("I've lived here", "2015.", listOf("since"), "starting point"),
        FillBlank("She speaks English very", ".", listOf("well"), "adverb of good"),
        FillBlank("The bridge was", "in 1890.", listOf("built"), "build → past participle"),
        FillBlank("Elephants are", "than horses.", listOf("bigger"), "comparative of big"),
        FillBlank("I don't have", "money with me.", listOf("any"), "negative sentence"),
        FillBlank("Would you mind", "the window?", listOf("opening"), "would you mind + -ing"),
        FillBlank("I'm looking forward to", "you.", listOf("seeing", "meeting"), "look forward to + -ing"),
        FillBlank("We arrived", "the airport at 6 a.m.", listOf("at"), "arrive + building"),
        FillBlank("He", "his homework yet.", listOf("hasn't done", "has not done"), "present perfect negative"),
        FillBlank("There are many", "in the park.", listOf("children"), "plural of child"),
        FillBlank("I usually", "breakfast at 7.", listOf("have", "eat"), "daily routine verb"),
        FillBlank("It's too cold", "swim today.", listOf("to"), "too + adjective + to"),
        FillBlank("She can", "three languages.", listOf("speak"), "modal + base verb"),
        FillBlank("Did you", "the email I sent?", listOf("get", "read", "see", "receive"), "did + base verb"),
        FillBlank("The meeting starts", "9 o'clock.", listOf("at"), "exact time"),
        FillBlank("They", "married for ten years.", listOf("have been", "'ve been"), "present perfect of be"),
        FillBlank("I was cooking", "the phone rang.", listOf("when"), "short interrupting action"),
    )

    val sentences = listOf(
        BuildSentence("I drink coffee every morning", "A daily habit"),
        BuildSentence("She has lived in London for five years", "Present perfect with for"),
        BuildSentence("Could you tell me where the station is", "Polite indirect question"),
        BuildSentence("We are going to visit our grandparents", "A future plan"),
        BuildSentence("If I had more time I would travel", "Second conditional"),
        BuildSentence("The report was finished yesterday", "Past passive"),
        BuildSentence("He usually goes to the gym after work", "Frequency adverb position"),
        BuildSentence("This is the best restaurant in town", "Superlative"),
        BuildSentence("I am looking forward to meeting you", "look forward to + -ing"),
        BuildSentence("They were watching TV when I arrived", "Past continuous"),
        BuildSentence("Do you know what time the film starts", "Indirect question"),
        BuildSentence("My sister is taller than me", "Comparative"),
        BuildSentence("Please send me the invoice by Friday", "Professional request"),
        BuildSentence("Although it was raining we went outside", "Contrast with although"),
        BuildSentence("I have never seen such a beautiful view", "Life experience"),
        BuildSentence("What do you usually do at the weekend", "WH- question"),
        BuildSentence("You should drink more water every day", "Advice with should"),
        BuildSentence("The train leaves at half past nine", "Timetable"),
        BuildSentence("I would like a table for two please", "Restaurant request"),
        BuildSentence("She speaks three languages fluently", "Adverb of manner"),
    )

    val words = listOf(
        WordCard("achieve", "verb", "to succeed in doing something after effort", "She achieved her goal of running a marathon."),
        WordCard("adapt", "verb", "to change to suit a new situation", "It took me a month to adapt to the new job."),
        WordCard("affordable", "adjective", "not too expensive", "We found an affordable hotel near the beach."),
        WordCard("appointment", "noun", "a fixed time to meet someone", "I have a dentist appointment at 3 p.m."),
        WordCard("available", "adjective", "free to use or to meet", "Are you available for a call tomorrow?"),
        WordCard("benefit", "noun", "an advantage or good result", "One benefit of cycling is better health."),
        WordCard("borrow", "verb", "to take something and give it back later", "Can I borrow your pen?"),
        WordCard("colleague", "noun", "a person you work with", "My colleagues helped me on my first day."),
        WordCard("confident", "adjective", "sure about your abilities", "She felt confident before the interview."),
        WordCard("convenient", "adjective", "easy and suitable", "Is Monday convenient for you?"),
        WordCard("deadline", "noun", "the last time to finish something", "The deadline for the project is Friday."),
        WordCard("decision", "noun", "a choice you make", "Moving abroad was a big decision."),
        WordCard("delighted", "adjective", "very pleased", "I'm delighted to meet you."),
        WordCard("efficient", "adjective", "working well without wasting time", "The new system is very efficient."),
        WordCard("encourage", "verb", "to give someone support and confidence", "My teacher encouraged me to keep practising."),
        WordCard("essential", "adjective", "absolutely necessary", "Water is essential for life."),
        WordCard("experience", "noun", "knowledge from doing something", "She has ten years of teaching experience."),
        WordCard("familiar", "adjective", "well known to you", "His face looks familiar."),
        WordCard("generous", "adjective", "happy to give to others", "It was generous of you to help."),
        WordCard("grateful", "adjective", "feeling thankful", "I'm grateful for your support."),
        WordCard("improve", "verb", "to make or become better", "Practice will improve your pronunciation."),
        WordCard("independent", "adjective", "not needing others", "She became independent after university."),
        WordCard("journey", "noun", "travel from one place to another", "The journey took six hours."),
        WordCard("knowledge", "noun", "information and understanding", "Reading increases your knowledge."),
        WordCard("lend", "verb", "to give something for a short time", "Could you lend me your umbrella?"),
        WordCard("maintain", "verb", "to keep something in good condition", "It's important to maintain a healthy lifestyle."),
        WordCard("negotiate", "verb", "to discuss to reach an agreement", "We negotiated a better price."),
        WordCard("opportunity", "noun", "a chance to do something", "This job is a great opportunity."),
        WordCard("patient", "adjective", "able to wait calmly", "Be patient — learning takes time."),
        WordCard("postpone", "verb", "to move to a later time", "They postponed the meeting until Monday."),
        WordCard("previous", "adjective", "happening before", "In my previous job I worked in sales."),
        WordCard("purchase", "verb", "to buy", "You can purchase tickets online."),
        WordCard("recommend", "verb", "to suggest something is good", "I recommend the chocolate cake."),
        WordCard("reliable", "adjective", "can be trusted", "He is a reliable team member."),
        WordCard("require", "verb", "to need", "This job requires good English."),
        WordCard("schedule", "noun", "a plan of times and activities", "My schedule is very busy this week."),
        WordCard("suggest", "verb", "to put forward an idea", "I suggest we take a break."),
        WordCard("temporary", "adjective", "lasting a short time", "This is only a temporary solution."),
        WordCard("valuable", "adjective", "very useful or worth a lot", "Thanks for your valuable feedback."),
        WordCard("wonder", "verb", "to think about something with curiosity", "I wonder what the weather will be like."),
    )

    val writingPrompts = listOf(
        WritingPrompt("My Daily Routine", "Describe your typical day from morning to night. Use frequency words like always, usually and sometimes.", 50, listOf("usually", "morning", "always")),
        WritingPrompt("My Best Holiday", "Write about the best trip you have taken. Where did you go, who were you with and what did you do?", 60, listOf("went", "visited", "was")),
        WritingPrompt("Email to a Friend", "Write an informal email inviting a friend to your birthday party. Include the date, time and place.", 50, listOf("party", "hope", "see")),
        WritingPrompt("My Future Plans", "What are your plans for the next five years? Use going to and will.", 50, listOf("going to", "will", "plan")),
        WritingPrompt("A Person I Admire", "Describe someone you admire and explain why. Use adjectives to describe their personality.", 60, listOf("because", "admire", "kind")),
        WritingPrompt("Complaint Email", "Write a polite email to a company about a product that arrived damaged. Ask for a replacement.", 60, listOf("dear", "damaged", "replacement")),
        WritingPrompt("Technology Opinion", "Do smartphones make our lives better or worse? Give your opinion with two reasons.", 70, listOf("opinion", "because", "however")),
        WritingPrompt("My Hometown", "Describe your hometown: its location, what it is famous for and what visitors should do there.", 60, listOf("famous", "there", "visit")),
        WritingPrompt("Learning English", "Why are you learning English? How will it help you in the future?", 50, listOf("learning", "because", "will")),
        WritingPrompt("Job Application", "Write a short cover letter for a job you would like. Mention your experience and skills.", 70, listOf("experience", "skills", "regards")),
    )

    val speaking = listOf(
        "Good morning, how are you today?",
        "Nice to meet you, my name is Alex.",
        "Could you tell me where the station is?",
        "I would like a table for two, please.",
        "I've been learning English for six months.",
        "Can I pay by card, please?",
        "What time does the meeting start?",
        "I think this is a great opportunity.",
        "Could you speak a little more slowly, please?",
        "Thank you very much for your help.",
        "I'm looking forward to working with you.",
        "The weather is beautiful today, isn't it?",
        "I usually go to work by train.",
        "If I had more time, I would travel the world.",
        "Please find attached the report you requested.",
        "Three thin thieves thought a thousand thoughts.",
        "She sells seashells by the seashore.",
        "I'm really interested in this position.",
        "Let me think about it and get back to you.",
        "Learning a little every day makes a big difference.",
    )

    val listening = listOf(
        "The train to London leaves at nine thirty.",
        "Please turn off your phone before the meeting.",
        "My brother works as an engineer in Canada.",
        "We are going to the beach this weekend.",
        "Could you send me the report by Friday?",
        "She has visited twelve countries so far.",
        "I usually have breakfast at seven o'clock.",
        "The museum is opposite the old library.",
        "It was raining when we left the hotel.",
        "Learning English opens many new doors.",
        "How much does this jacket cost?",
        "They have lived here since twenty fifteen.",
        "If it rains tomorrow, we will stay at home.",
        "The meeting has been moved to Thursday.",
        "Thank you for your patience and understanding.",
    )

    // ------------------------------------------------------------------ simple checks

    fun normalize(s: String): String =
        s.lowercase().replace("’", "'").replace(Regex("[^a-z0-9' ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Word-level similarity between 0 and 100 (order-aware LCS over words). */
    fun similarity(target: String, said: String): Int {
        val a = normalize(target).split(" ").filter { it.isNotEmpty() }
        val b = normalize(said).split(" ").filter { it.isNotEmpty() }
        if (a.isEmpty() || b.isEmpty()) return 0
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in 1..a.size) for (j in 1..b.size) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1 else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
        val lcs = dp[a.size][b.size]
        return (lcs * 200 / (a.size + b.size)).coerceIn(0, 100)
    }
}
