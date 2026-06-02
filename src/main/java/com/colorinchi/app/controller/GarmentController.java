package com.colorinchi.app.controller;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.colorimetry.model.ColorProfile;
import com.colorinchi.app.colorimetry.service.ColorSeasonClassifier;
import com.colorinchi.app.dto.AiClassificationResponse;
import com.colorinchi.app.dto.AiRecommendationResponse;
import com.colorinchi.app.dto.DashboardStats;
import com.colorinchi.app.dto.GarmentReviewForm;
import com.colorinchi.app.dto.InspirationLook;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.service.AiClassificationService;
import com.colorinchi.app.service.GarmentCompatibilityService;
import com.colorinchi.app.service.AiRecommendationService;
import com.colorinchi.app.service.GarmentService;
import com.colorinchi.app.service.InspirationService;
import com.colorinchi.app.service.WeekPlanService;
import com.colorinchi.app.upload.ImageStorageService;

@Controller
public class GarmentController {

    private final GarmentService garmentService;
    private final ImageStorageService imageStorageService;
    private final AiClassificationService aiClassificationService;
    private final AiRecommendationService aiRecommendationService;
    private final WeekPlanService weekPlanService;
    private final GarmentCompatibilityService garmentCompatibilityService;
    private final InspirationService inspirationService;
    private final WardrobeProperties wardrobeProperties;
    private final ColorSeasonClassifier seasonClassifier;

    public GarmentController(
            GarmentService garmentService,
            ImageStorageService imageStorageService,
            AiClassificationService aiClassificationService,
            AiRecommendationService aiRecommendationService,
            WeekPlanService weekPlanService,
            GarmentCompatibilityService garmentCompatibilityService,
            InspirationService inspirationService,
            WardrobeProperties wardrobeProperties,
            ColorSeasonClassifier seasonClassifier) {
        this.garmentService = garmentService;
        this.imageStorageService = imageStorageService;
        this.aiClassificationService = aiClassificationService;
        this.aiRecommendationService = aiRecommendationService;
        this.weekPlanService = weekPlanService;
        this.garmentCompatibilityService = garmentCompatibilityService;
        this.inspirationService = inspirationService;
        this.wardrobeProperties = wardrobeProperties;
        this.seasonClassifier = seasonClassifier;
    }

    @ModelAttribute("categories")
    List<String> categories() {
        return wardrobeProperties.categories();
    }

    @GetMapping("/")
    String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    String dashboard(Model model) {
        long plannedDays = weekPlanService.countDistinctDaysPlanned();
        long plannedItems = weekPlanService.countPlanned();
        DashboardStats stats = garmentService.getDashboardStats(plannedDays, plannedItems);
        model.addAttribute("stats", stats);
        model.addAttribute("garmentCount", stats.totalGarments());
        model.addAttribute("latestGarments", garmentService.latest());
        model.addAttribute("usagePercent", stats.usagePercent());
        model.addAttribute("usageMessage", usageMessage(stats.usagePercent()));
        return "dashboard";
    }

    private String usageMessage(int pct) {
        if (pct == 0) return "Empieza a planificar";
        if (pct < 50) return "Vas bien, sigue sumando";
        if (pct < 100) return "Buen ritmo";
        return "Semana completa";
    }

    @GetMapping("/wardrobe")
    String wardrobe(Model model) {
        model.addAttribute("garments", garmentService.all());
        model.addAttribute("activeCategory", "");
        return "wardrobe";
    }

    @GetMapping("/wardrobe/filter")
    String wardrobeFilter(@RequestParam(required = false) String category, Model model) {
        if ("favoritos".equals(category)) {
            model.addAttribute("garments", garmentService.favorites());
        } else {
            model.addAttribute("garments", garmentService.filterByCategory(category));
        }
        model.addAttribute("activeCategory", category != null ? category : "");
        return "wardrobe :: grid";
    }

    @PostMapping("/wardrobe/{id}/favorite")
    String toggleFavorite(@PathVariable Long id, 
                          @RequestParam(defaultValue = "card") String variant,
                          @RequestParam(required = false) String category,
                          Model model) {
        garmentService.toggleFavorite(id);
        Garment garment = garmentService.get(id);
        model.addAttribute("garment", garment);
        if ("detail".equals(variant)) {
            return "garment-detail :: favDetailButton";
        }
        if ("favoritos".equals(category)) {
            model.addAttribute("garments", garmentService.favorites());
        } else if (category == null || category.isBlank()) {
            model.addAttribute("garments", garmentService.all());
        } else {
            model.addAttribute("garments", garmentService.filterByCategory(category));
        }
        model.addAttribute("activeCategory", category != null ? category : "");
        return "wardrobe :: grid";
    }

    @DeleteMapping("/wardrobe/{id}")
    @ResponseBody
    String deleteGarment(@PathVariable Long id,
                         @RequestParam(defaultValue = "card") String source,
                         HttpServletResponse response) {
        garmentService.delete(id);
        if ("detail".equals(source)) {
            response.setHeader("HX-Redirect", "/wardrobe");
        }
        return "";
    }

    @GetMapping("/wardrobe/new")
    String newGarment() {
        return "garment-new";
    }

    @PostMapping("/wardrobe/analyze")
    String analyze(@RequestParam("image") MultipartFile image, Model model, RedirectAttributes redirectAttributes) {
        try {
            String imageUrl = imageStorageService.store(image);
            AiClassificationResponse ai = aiClassificationService.classify(imageUrl);
            GarmentReviewForm form = new GarmentReviewForm();
            form.setImageUrl(imageUrl);
            form.setCategory(ai.hasPrediction() ? normalizeCategory(ai.type()) : "Otro");
            form.setColorName(ai.hasPrediction() ? ai.colorName() : "");
            form.setColorHex(ai.colorHex());
            form.setName(defaultName(form.getCategory(), form.getColorName()));
            form.setAiType(ai.type());
            form.setAiColorName(ai.colorName());
            form.setAiColorHex(ai.colorHex());
            form.setAiConfidence(ai.confidence());
            form.setAiModel(ai.model());
            model.addAttribute("garmentReviewForm", form);
            model.addAttribute("aiError", ai.error());
            return "garment-confirm";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/wardrobe/new";
        }
    }

    @PostMapping("/wardrobe")
    String create(@Valid @ModelAttribute GarmentReviewForm garmentReviewForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "garment-confirm";
        }
        Garment garment = garmentService.create(garmentReviewForm);
        return "redirect:/wardrobe/" + garment.getId();
    }

    @GetMapping("/wardrobe/{id}")
    String detail(@PathVariable Long id, Model model) {
        Garment garment = garmentService.get(id);
        model.addAttribute("garment", garment);
        model.addAttribute("compatibleGarments", garmentCompatibilityService.findCompatible(garment));
        model.addAttribute("companionGarments", weekPlanService.findCompanionGarments(id));

        addColorSeason(garment, model);

        return "garment-detail";
    }

    private void addColorSeason(Garment garment, Model model) {
        String hex = garment.getColorHex();
        if (hex != null && !hex.isBlank()) {
            try {
                ColorProfile profile = seasonClassifier.classify(hex);
                if (profile.season() != null) {
                    model.addAttribute("garmentSeason", profile.season().displayName());
                }
            } catch (Exception e) {
                // ignore — no season badge
            }
        }
    }

    @GetMapping("/wardrobe/{id}/edit")
    String editForm(@PathVariable Long id, Model model) {
        Garment garment = garmentService.get(id);
        GarmentReviewForm form = new GarmentReviewForm();
        form.setId(garment.getId());
        form.setName(garment.getName());
        form.setCategory(garment.getCategory());
        form.setColorName(garment.getColorName());
        form.setColorHex(garment.getColorHex());
        form.setMaterial(garment.getMaterial());
        form.setSeason(garment.getSeason());
        form.setImageUrl(garment.getImageUrl());
        form.setAiType(garment.getAiType());
        form.setAiColorName(garment.getAiColorName());
        form.setAiColorHex(garment.getAiColorHex());
        form.setAiConfidence(garment.getAiConfidence());
        form.setAiModel(garment.getAiModel());
        model.addAttribute("garmentReviewForm", form);
        model.addAttribute("garment", garment);
        return "garment-edit";
    }

    @PutMapping("/wardrobe/{id}")
    String update(@PathVariable Long id,
                  @Valid @ModelAttribute GarmentReviewForm form,
                  BindingResult bindingResult,
                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("garment", garmentService.get(id));
            return "garment-edit";
        }
        Garment garment = garmentService.update(id, form);
        return "redirect:/wardrobe/" + garment.getId();
    }

    @PostMapping("/wardrobe/seed")
    String seedWardrobe() {
        garmentService.seed();
        return "redirect:/wardrobe";
    }

    @GetMapping("/inspiration")
    String inspiration(Model model) {
        List<InspirationLook> inspirations = inspirationService.getAll();
        Set<String> allTags = inspirationService.getAllTags();
        model.addAttribute("inspirations", inspirations);
        model.addAttribute("allTags", allTags);
        return "inspiration";
    }

    @GetMapping("/recommendation")
    String recommendation(Model model) {
        AiRecommendationResponse rec = aiRecommendationService.generate();
        model.addAttribute("recommendations", rec.outfits());
        return "recommendation";
    }

    @GetMapping("/weekly-plan")
    String weeklyPlan(Model model) {
        model.addAttribute("days", wardrobeProperties.days());
        model.addAttribute("plans", weekPlanService.getPlansByDay());
        model.addAttribute("allGarments", garmentService.all());
        return "weekly-plan";
    }

    @PostMapping("/weekly-plan/assign")
    @ResponseBody
    String assignGarment(@RequestParam Long garmentId,
                         @RequestParam String dayOfWeek,
                         @RequestParam int position) {
        weekPlanService.assignGarment(garmentId, dayOfWeek, position);
        return "";
    }

    @DeleteMapping("/weekly-plan/{id}")
    @ResponseBody
    String removeFromPlan(@PathVariable Long id) {
        weekPlanService.remove(id);
        return "";
    }

    @PutMapping("/weekly-plan/reorder")
    @ResponseBody
    String reorderDay(@RequestParam String dayOfWeek, @RequestParam List<Long> order) {
        weekPlanService.reorderDay(dayOfWeek, order);
        return "";
    }

    @GetMapping("/profile")
    String profile(Model model) {
        long plannedDays = weekPlanService.countDistinctDaysPlanned();
        long plannedItems = weekPlanService.countPlanned();
        DashboardStats stats = garmentService.getDashboardStats(plannedDays, plannedItems);
        long maxCategoryCount = stats.categoryBreakdown().stream()
            .mapToLong(DashboardStats.CategoryCount::count)
            .max().orElse(1);
        model.addAttribute("stats", stats);
        model.addAttribute("maxCategoryCount", maxCategoryCount);
        model.addAttribute("garmentCount", stats.totalGarments());
        model.addAttribute("usagePercent", stats.usagePercent());
        model.addAttribute("usageMessage", usageMessage(stats.usagePercent()));
        model.addAttribute("topColors", garmentService.getTopColors());
        return "profile-stats";
    }

    private String defaultName(String category, String colorName) {
        if (category == null || category.isBlank()) {
            return "Prenda nueva";
        }
        if (colorName == null || colorName.isBlank()) {
            return category;
        }
        return category + " " + colorName;
    }

    private String normalizeCategory(String aiType) {
        if (aiType == null || aiType.isBlank()) {
            return "Otro";
        }
        return wardrobeProperties.categories().stream()
                .filter(category -> category.equalsIgnoreCase(aiType.trim()))
                .findFirst()
                .orElse("Otro");
    }
}
