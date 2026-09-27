package com.gymcrm.storage;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
public class StorageInitializerBeanPostProcessor implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(StorageInitializerBeanPostProcessor.class);

    @Value("${storage.data.path}")
    private String dataPath;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof Storage storage) {
            log.info("Initializing storage from file: {}", dataPath);
            loadData(storage);
        }
        return bean;
    }

    private void loadData(Storage storage) {
        try (BufferedReader reader = new BufferedReader(new FileReader(dataPath))) {
            String line;
            Set<String> usernames = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 1) continue;

                String type = parts[0];
                if ("TRAINEE".equals(type)) {
                    Trainee trainee = new Trainee(parts[1], parts[2], LocalDate.parse(parts[3]), parts[4]);
                    trainee.setUsername(UserUtils.generateUsername(trainee.getFirstName(), trainee.getLastName(), usernames));
                    trainee.setPassword(UserUtils.generatePassword());
                    usernames.add(trainee.getUsername());
                    storage.put("Trainee", storage.nextId("Trainee"), trainee);
                } else if ("TRAINER".equals(type)) {
                    Trainer trainer = new Trainer(parts[1], parts[2], new TrainingType(parts[3]));
                    trainer.setUsername(UserUtils.generateUsername(trainer.getFirstName(), trainer.getLastName(), usernames));
                    trainer.setPassword(UserUtils.generatePassword());
                    usernames.add(trainer.getUsername());
                    storage.put("Trainer", storage.nextId("Trainer"), trainer);
                }
            }
        } catch (IOException e) {
            log.warn("Could not load initial data from {}: {}", dataPath, e.getMessage());
        } catch (Exception e) {
            log.error("Error during storage initialization", e);
        }
    }
}
