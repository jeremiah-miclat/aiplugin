package com.riftforged.aicompanion;

/** Plain bot-logic chat lines (no AI call) — mirrors the MESSAGES object in watcher.js. */
public final class Messages {
    private final Persona persona;
    private final BotConfig config;

    public Messages(Persona persona, BotConfig config) {
        this.persona = persona;
        this.config = config;
    }

    public String askFailureFallback() {
        return persona == Persona.TRASHTALK
            ? "ugh, my brain just blue-screened. try again, I guess."
            : "sorry, I couldn't come up with an answer just now — please try asking again!";
    }

    public String itemLimitHit(int max) {
        return persona == Persona.TRASHTALK
            ? "nope, you already burned all " + max + " of your item requests for today. come back tomorrow, champ."
            : "Sorry, you've hit your limit of " + max + " item requests per 24 hours — please try again tomorrow!";
    }

    /** Overrides whatever reply text the model produced when it tried to give an item while the
     *  master itemGiving.enabled toggle is off — see resolveSubpart's runtime safety net. Says
     *  the feature is OFF, deliberately distinct from itemLimitHit's "you're out for today" so a
     *  player isn't told to just wait and try tomorrow when nothing will change. */
    public String itemGivingDisabled() {
        return persona == Persona.TRASHTALK
            ? "yeah, giving out items is turned off here entirely. don't shoot the messenger."
            : "Sorry, item giving is turned off on this server — I can still help with questions though!";
    }

    public String lowRemaining(int n) {
        String plural = n == 1 ? "" : "s";
        return persona == Persona.TRASHTALK
            ? "you've got " + n + " item request" + plural + " left today. don't waste 'em on something dumb."
            : "heads up — you have " + n + " item request" + plural + " left today.";
    }

    public String cooldownNotice(long cooldownSeconds) {
        return persona == Persona.TRASHTALK
            ? "that one's not getting answered — you're on cooldown. wait " + cooldownSeconds + "s and try again."
            : "sorry, that message won't be answered — please wait " + cooldownSeconds + "s and try again!";
    }

    public String queueFullNotice() {
        return persona == Persona.TRASHTALK
            ? "that one's not getting answered — everyone's asking me stuff right now. try again in a bit."
            : "sorry, that message won't be answered — I'm swamped with questions right now, please try again in a bit!";
    }

    public String joinFallbackGreeting() {
        return persona == Persona.TRASHTALK
            ? "oh great, another one. welcome, I guess."
            : "Welcome to the server!";
    }

    public String joinTip() {
        String prefix = config.askPrefix();
        if (!config.itemGivingEnabled()) {
            return persona == Persona.TRASHTALK
                ? "type " + prefix + " <anything> if you want to talk to me — questions are free. " +
                    "don't bother asking for items though, that's turned off here."
                : "Tip: type " + prefix + " <anything> to chat or ask questions — totally unlimited!";
        }
        int max = config.itemGivingMaxPerDay();
        return persona == Persona.TRASHTALK
            ? "type " + prefix + " <anything> if you want to talk to me — questions are free, but "
                + "don't push your luck on item requests, capped at " + max + " a day."
            : "Tip: type " + prefix + " <anything> to chat or ask questions — totally unlimited! "
                + "Only asking me to give you an item is capped, at " + max + " per 24h.";
    }
}
