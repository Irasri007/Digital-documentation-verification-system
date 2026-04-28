package com.ddvs;

/**
 * Detects the likely issuing authority from a document's filename and MIME type.
 *
 * Rules are checked in priority order — first match wins.
 * If nothing matches, falls back to the user-supplied issuer value.
 */
public class IssuerDetector {

    /**
     * Returns the detected issuer name, or {@code fallback} if no rule matched.
     *
     * @param fileName  original filename (e.g. "aadhaar_card.pdf")
     * @param mimeType  MIME type       (e.g. "application/pdf")
     * @param fallback  value to use when nothing is detected
     */
    public static String detect(String fileName, String mimeType, String fallback) {
        if (fileName == null) return fallback;
        String lower = fileName.toLowerCase().replaceAll("[_\\-\\s]+", " ");

        // ── Government / Identity documents ──────────────────────────────────
        if (matches(lower, "aadhaar", "aadhar", "uid card", "uidai"))
            return "Government of India (UIDAI)";

        if (matches(lower, "pan card", "pancard", "permanent account"))
            return "Income Tax Department, Government of India";

        if (matches(lower, "passport"))
            return "Ministry of External Affairs, Government of India";

        if (matches(lower, "voter id", "voter card", "epic card", "election card"))
            return "Election Commission of India";

        if (matches(lower, "driving licence", "driving license", "dl cert", "rc book",
                           "registration certificate"))
            return "Ministry of Road Transport, Government of India";

        if (matches(lower, "birth certificate", "birth cert"))
            return "Municipal Corporation / Government of India";

        if (matches(lower, "ration card"))
            return "Department of Food & Civil Supplies, Government of India";

        if (matches(lower, "income certificate", "caste certificate", "domicile",
                           "residence certificate"))
            return "State Government of India";

        if (matches(lower, "covid", "vaccination", "vaccine certificate", "cowin"))
            return "Ministry of Health, Government of India (CoWIN)";

        // ── Academic / Education ──────────────────────────────────────────────
        if (matches(lower, "10th", "class x", "ssc result", "cbse 10", "matric",
                           "secondary certificate", "high school certificate"))
            return "CBSE / State Board (Secondary Education)";

        if (matches(lower, "12th", "class xii", "class 12", "hsc result", "cbse 12",
                           "intermediate", "senior secondary", "higher secondary"))
            return "CBSE / State Board (Senior Secondary Education)";

        if (matches(lower, "marksheet", "mark sheet", "result", "grade card",
                           "scorecard", "transcript"))
            return detectCollegeOrSchool(lower, fallback);

        if (matches(lower, "degree certificate", "degree cert", "convocation"))
            return detectCollegeOrSchool(lower, fallback);

        if (matches(lower, "bonafide", "enrollment", "enrolment", "student id",
                           "college id", "university id", "admit card"))
            return detectCollegeOrSchool(lower, fallback);

        if (matches(lower, "jee", "neet", "cuet", "upsc", "ssc cgl", "gate result",
                           "cat result"))
            return "National Testing Agency / Examination Board";

        if (matches(lower, "iit", "iim", "nit", "bits pilani"))
            return detectPremiereInstitute(lower, fallback);

        // ── Employment / Professional ─────────────────────────────────────────
        if (matches(lower, "offer letter", "appointment letter", "joining letter"))
            return "Employer Organisation";

        if (matches(lower, "salary slip", "payslip", "pay stub", "salary certificate"))
            return "Employer / HR Department";

        if (matches(lower, "experience certificate", "relieving letter",
                           "service certificate"))
            return "Employer Organisation";

        if (matches(lower, "internship certificate", "internship letter",
                           "intern completion"))
            return "Internship Issuing Organisation";

        if (matches(lower, "ca certificate", "chartered accountant", "icai"))
            return "Institute of Chartered Accountants of India (ICAI)";

        // ── Financial / Banking ───────────────────────────────────────────────
        if (matches(lower, "bank statement", "account statement", "passbook"))
            return "Banking Institution";

        if (matches(lower, "itr", "income tax return", "form 16", "form16"))
            return "Income Tax Department, Government of India";

        if (matches(lower, "insurance", "policy document", "premium receipt"))
            return "Insurance Company";

        // ── Medical ───────────────────────────────────────────────────────────
        if (matches(lower, "medical certificate", "fitness certificate",
                           "disability certificate"))
            return "Registered Medical Practitioner / Hospital";

        if (matches(lower, "prescription", "discharge summary", "lab report",
                           "pathology report"))
            return "Hospital / Diagnostic Centre";

        // ── Property / Legal ──────────────────────────────────────────────────
        if (matches(lower, "property", "sale deed", "registry", "land record",
                           "khata", "mutation"))
            return "Sub-Registrar Office / Revenue Department";

        if (matches(lower, "noc", "no objection"))
            return "Issuing Authority (NOC)";

        // ── Nothing matched — return fallback ─────────────────────────────────
        return fallback;
    }

    // ── Helper: check if any keyword appears in the lowercase filename ────────
    private static boolean matches(String lower, String... keywords) {
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    // ── Try to detect college/school name from filename ───────────────────────
    private static String detectCollegeOrSchool(String lower, String fallback) {
        // Well-known institutions
        if (lower.contains("geu") || lower.contains("graphic era"))
            return "Graphic Era University";
        if (lower.contains("iit"))  return detectIIT(lower);
        if (lower.contains("nit"))  return "National Institute of Technology";
        if (lower.contains("du") || lower.contains("delhi university"))
            return "University of Delhi";
        if (lower.contains("vtu"))  return "Visvesvaraya Technological University";
        if (lower.contains("anna university")) return "Anna University";
        if (lower.contains("mumbai university") || lower.contains("mu "))
            return "University of Mumbai";
        if (lower.contains("pune university") || lower.contains("sppu"))
            return "Savitribai Phule Pune University";
        if (lower.contains("cbse")) return "Central Board of Secondary Education (CBSE)";
        if (lower.contains("icse") || lower.contains("cisce"))
            return "Council for the Indian School Certificate Examinations (CISCE)";
        // Fallback for academic docs
        return fallback.isBlank() ? "Academic Institution" : fallback;
    }

    private static String detectIIT(String lower) {
        if (lower.contains("bombay") || lower.contains("iitb")) return "IIT Bombay";
        if (lower.contains("delhi") || lower.contains("iitd"))  return "IIT Delhi";
        if (lower.contains("madras") || lower.contains("iitm")) return "IIT Madras";
        if (lower.contains("kanpur") || lower.contains("iitk")) return "IIT Kanpur";
        if (lower.contains("kharagpur") || lower.contains("iitkgp")) return "IIT Kharagpur";
        if (lower.contains("roorkee") || lower.contains("iitr")) return "IIT Roorkee";
        return "Indian Institute of Technology (IIT)";
    }

    private static String detectPremiereInstitute(String lower, String fallback) {
        if (lower.contains("iit"))  return detectIIT(lower);
        if (lower.contains("iim"))  return "Indian Institute of Management (IIM)";
        if (lower.contains("bits")) return "BITS Pilani";
        return fallback;
    }
}

