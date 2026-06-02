package com.lunatech.dragonegghunt.config;

import com.lunatech.dragonegghunt.config.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardConfigTest {

    @Test
    @DisplayName("Should pass validation with default valid configuration")
    void testValidDefaultConfig() {
        DashboardConfig config = new DashboardConfig();
        assertDoesNotThrow(config::validate);
    }

    @Test
    @DisplayName("Should throw validation exception if GUI rows is less than 1")
    void testInvalidRowsTooSmall() {
        DashboardConfig config = new DashboardConfig();
        config.gui.rows = 0;

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, config::validate);
        assertTrue(exception.getMessage().contains("gui.rows must be between 1 and 6"));
    }

    @Test
    @DisplayName("Should throw validation exception if GUI rows is greater than 6")
    void testInvalidRowsTooLarge() {
        DashboardConfig config = new DashboardConfig();
        config.gui.rows = 7;

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, config::validate);
        assertTrue(exception.getMessage().contains("gui.rows must be between 1 and 6"));
    }

    @Test
    @DisplayName("Should throw validation exception if Egg Status slot is out of bounds")
    void testEggStatusSlotOutOfBounds() {
        DashboardConfig config = new DashboardConfig();
        config.gui.rows = 3;
        config.gui.eggStatusSlot = 27; // Max index is 26 for 3 rows (3 * 9 = 27 slots)

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, config::validate);
        assertTrue(exception.getMessage().contains("gui.egg-status-slot must be within inventory bounds"));
    }

    @Test
    @DisplayName("Should throw validation exception if Admin Actions slot is out of bounds")
    void testAdminActionsSlotOutOfBounds() {
        DashboardConfig config = new DashboardConfig();
        config.gui.rows = 2;
        config.gui.adminActionsSlot = -1;

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, config::validate);
        assertTrue(exception.getMessage().contains("gui.admin-actions-slot must be within inventory bounds"));
    }

    @Test
    @DisplayName("Should throw validation exception if Audit Logs slot is out of bounds")
    void testAuditLogsSlotOutOfBounds() {
        DashboardConfig config = new DashboardConfig();
        config.gui.rows = 4;
        config.gui.auditLogsSlot = 36; // 4 * 9 = 36 slots, max index is 35

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, config::validate);
        assertTrue(exception.getMessage().contains("gui.audit-logs-slot must be within inventory bounds"));
    }
}
