package com.colorinchi.app.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ChatIntentClassifier {

    public enum Intent {
        WARDROBE_QUESTION,
        STYLING_ADVICE,
        COLOR_ADVICE,
        OUTFIT_REQUEST,
        GENERAL_CHAT
    }

    private static final List<Pattern> OUTFIT_REQUEST_PATTERNS = List.of(
        Pattern.compile("(pick|choose|select|suggest|recommend|give|create|make|armá?|dame|elegí?|creá?|sugerí?|recomendá?)"
            + ".*(outfit|look|ensemble|what (should|do) I wear|qué (me pongo|uso|visto)|un (outfit|look|conjunto))",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(outfit|look).*(para mí|for me|que me queda|que me pondría)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(what should I wear|qué me pongo|qué ropa me|armame|preparame|organizame)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(elige|elegí|escoge|escogé|seleccioná) (por mí|por mi|un outfit|una combinación|qué ropa)",
            Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> WARDROBE_QUESTION_PATTERNS = List.of(
        Pattern.compile("(tengo|tenfo?|do I have|what.*(garment|prenda|ropa|tengo)|list.*(garment|prenda|item))",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(muestrame|enseñame|mostrame|show me).*(armario|guardarropa|wardrobe|ropa|prendas)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(cuántos?|cuantas?|how many).*(tengo|have|poseo)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("^qué (prendas?|ropa|cosas) (tengo|hay|poseo)",
            Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> STYLING_ADVICE_PATTERNS = List.of(
        Pattern.compile("(cómo|como|how (to|should|do|can)|ways to|goes (with|well)) (combin|wear|style|pair|use|us|pon|vest)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(combina|va con|match|matches|pair|paired|goes with).*(esta|esto|prenda|skirt|shirt|pants|dress|jacket)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(styling|style|styling tip|consejo|sugerencia).*(advice|tip|como|cómo|estilo|moda)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(consejos|consejo).*(estilo|moda|look|ropa)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("qué (me|te) (pongo|puedo|recomendás|recomendas|recomiendas|sugerís) con",
            Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> COLOR_ADVICE_PATTERNS = List.of(
        Pattern.compile("(color|colores|tono|tonos|palette|paleta|shade|shades)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(match|matches|harmonize|armoniza|pega|pegan).*(color|colores|tono)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(qué color|what color|que color|de qué color|colores que|qué tonos)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(colorimetr|teoría del color|color theory)",
            Pattern.CASE_INSENSITIVE)
    );

    public Intent classify(String message) {
        if (message == null || message.isBlank()) {
            return Intent.GENERAL_CHAT;
        }

        // Outfit requests are the most restrictive — check first
        if (matchesAny(message, OUTFIT_REQUEST_PATTERNS)) {
            return Intent.OUTFIT_REQUEST;
        }

        if (matchesAny(message, WARDROBE_QUESTION_PATTERNS)) {
            return Intent.WARDROBE_QUESTION;
        }

        if (matchesAny(message, STYLING_ADVICE_PATTERNS)) {
            return Intent.STYLING_ADVICE;
        }

        if (matchesAny(message, COLOR_ADVICE_PATTERNS)) {
            return Intent.COLOR_ADVICE;
        }

        return Intent.GENERAL_CHAT;
    }

    public boolean isOutfitRequest(String message) {
        return classify(message) == Intent.OUTFIT_REQUEST;
    }

    private boolean matchesAny(String message, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }
}
