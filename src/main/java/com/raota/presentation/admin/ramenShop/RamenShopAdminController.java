package com.raota.presentation.admin.ramenShop;

import com.raota.presentation.admin.ramenShop.dto.RamenShopAdminForm;
import com.raota.application.admin.ramenShop.RamenShopAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ramen-shops")
@RequiredArgsConstructor
public class RamenShopAdminController {

    private final RamenShopAdminService ramenShopAdminService;

    @GetMapping
    public String adminPage(@RequestParam(required = false) Long shopId, Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", shopId == null ? RamenShopAdminForm.empty() : ramenShopAdminService.getForm(shopId));
        }
        populate(model, shopId);
        return "admin/ramen-shops";
    }

    @PostMapping
    public String createShop(@Valid @ModelAttribute("form") RamenShopAdminForm form, BindingResult bindingResult,
                             Model model, RedirectAttributes redirectAttributes) {
        form.ensureMenuRows();
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", form);
            populate(model, null);
            return "admin/ramen-shops";
        }

        try {
            Long createdId = ramenShopAdminService.createShop(form);
            redirectAttributes.addFlashAttribute("successMessage", "라멘집이 추가되었습니다.");
            redirectAttributes.addAttribute("shopId", createdId);
            return "redirect:/admin/ramen-shops";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("form.error", exception.getMessage());
            model.addAttribute("form", form);
            populate(model, null);
            return "admin/ramen-shops";
        }
    }

    @PostMapping("/{shopId}")
    public String updateShop(@PathVariable Long shopId, @Valid @ModelAttribute("form") RamenShopAdminForm form,
                             BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        form.ensureMenuRows();
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", form);
            populate(model, shopId);
            return "admin/ramen-shops";
        }

        try {
            ramenShopAdminService.updateShop(shopId, form);
            redirectAttributes.addFlashAttribute("successMessage", "라멘집 정보가 수정되었습니다.");
            redirectAttributes.addAttribute("shopId", shopId);
            return "redirect:/admin/ramen-shops";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("form.error", exception.getMessage());
            model.addAttribute("form", form);
            populate(model, shopId);
            return "admin/ramen-shops";
        }
    }

    @PostMapping("/{shopId}/delete")
    public String deleteShop(@PathVariable Long shopId, RedirectAttributes redirectAttributes) {
        ramenShopAdminService.deleteShop(shopId);
        redirectAttributes.addFlashAttribute("successMessage", "라멘집이 삭제되었습니다.");
        return "redirect:/admin/ramen-shops";
    }

    private void populate(Model model, Long selectedShopId) {
        model.addAttribute("shops", ramenShopAdminService.getShopSummaries());
        model.addAttribute("selectedShopId", selectedShopId);
        model.addAttribute("editMode", selectedShopId != null);
    }
}
