package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;
    private SecurityService securityService;

    @Mock
    SecurityRepository securityRepository;

    @Mock
    ImageService imageService;

    private Sensor getNewSensor() {
        return new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }

    private Set<Sensor> getSensors(boolean active, int count) {
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0; i < count; i++) {
            sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.DOOR));
        }
        sensors.forEach(it -> it.setActive(active));
        return sensors;
    }

    @BeforeEach
    void init() {
        sensor = getNewSensor();
        securityService = new SecurityService(securityRepository, imageService);
    }

    // Test 1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmArmed_sensorActivated_changeToPending(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmArmedAndPending_sensorActivated_changeToAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 3
    @Test
    void alarmPending_allSensorsInactive_returnToNoAlarm() {
        Set<Sensor> sensors = getSensors(false,4);
        Sensor firstSensor = sensors.iterator().next();
        firstSensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(firstSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 4
    @Test
    void alarmActive_sensorChange_noEffect() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        //Case 1: sensor activated
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

        //Case 2: sensor deactivated
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test 5
    @Test
    void alarmPending_sensorActivatedWhileAlreadyActive_changeToAlarm() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 6
    @Test
    void sensorDeactivatedWhileAlreadyInactive_noEffect() {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test 7
    @Test
    void alarmArmedHome_identifiesImageContainingCat_changeToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 8
    @Test
    void allSensorsInactive_identifiesImageWithoutCat_changeToNoAlarm() {
        Set<Sensor> sensors = getSensors(false,4);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 9
    @Test
    void alarmDisarmed_changeToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmArmed_resetAllSensorsToInactive(ArmingStatus armingStatus) {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Set<Sensor> sensors = getSensors(true, 4);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(armingStatus);
        sensors.forEach(it -> assertFalse(it.getActive()));
    }

    //Test 11
    @Test
    void alarmArmedHome_identifiesCat_changeToAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

}
