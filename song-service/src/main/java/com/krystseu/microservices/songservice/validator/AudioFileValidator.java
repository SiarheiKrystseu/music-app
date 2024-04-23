package com.krystseu.microservices.songservice.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

@Component
public class AudioFileValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return MultipartFile.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        MultipartFile file = (MultipartFile) target;
        if (!isValidAudioFile(file)) {
            errors.rejectValue("file", "invalid.file.type", "Uploaded file is not a valid MP3 audio file");
        }
    }

    private boolean isValidAudioFile(MultipartFile file) {
        try {
            AudioSystem.getAudioFileFormat(file.getInputStream());
            return true;
        } catch (UnsupportedAudioFileException | IOException e) {
            return false;
        }
    }
}