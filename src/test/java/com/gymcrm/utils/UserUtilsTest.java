package com.gymcrm.utils;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserUtilsTest {

    @Test
    void testGenerateUsername() {
        Set<String> existing = new HashSet<>();
        String u1 = UserUtils.generateUsername("John", "Smith", existing);
        assertEquals("John.Smith", u1);
        
        existing.add(u1);
        String u2 = UserUtils.generateUsername("John", "Smith", existing);
        assertEquals("John.Smith1", u2);
        
        existing.add(u2);
        String u3 = UserUtils.generateUsername("John", "Smith", existing);
        assertEquals("John.Smith2", u3);
    }

    @Test
    void testGeneratePassword() {
        String p1 = UserUtils.generatePassword();
        assertEquals(10, p1.length());
        String p2 = UserUtils.generatePassword();
        assertNotEquals(p1, p2);
    }

    @Test
    void testCollectUsernames() {
        Trainee t1 = new Trainee(); t1.setUsername("t1");
        Trainer tr1 = new Trainer(); tr1.setUsername("tr1");
        Trainer tr2 = new Trainer(); // no username
        
        Set<String> usernames = UserUtils.collectUsernames(Collections.singletonList(t1), Arrays.asList(tr1, tr2));
        assertEquals(2, usernames.size());
        assertTrue(usernames.contains("t1"));
        assertTrue(usernames.contains("tr1"));
    }
}
