import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withRepeat,
  interpolate,
} from 'react-native-reanimated';

// Pulsing Circle Component for the recording animation
const PulsingCircle = ({ isRecording }) => {
  const animation = useSharedValue(0);

  useEffect(() => {
    if (isRecording) {
      animation.value = withRepeat(withTiming(1, { duration: 1200 }), -1, false);
    } else {
      animation.value = withTiming(0, { duration: 500 });
    }
  }, [isRecording]);

  const animatedStyle = useAnimatedStyle(() => {
    const scale = interpolate(animation.value, [0, 1], [1, 1.8]);
    const opacity = interpolate(animation.value, [0, 1], [0.6, 0]);
    return {
      transform: [{ scale }],
      opacity: opacity,
    };
  });

  return <Animated.View style={[styles.pulsingCircle, animatedStyle]} />;
};

export default function AiBotScreen() {
  const [isRecording, setIsRecording] = useState(false);

  const handleRecordButtonPress = () => {
    setIsRecording((prev) => !prev);
    // TODO: Add actual audio recording logic here
  };

  return (
    <View style={styles.container}>
      <View style={styles.botProfileContainer}>
        {/* Wrapper View to align both circles */}
        <View style={styles.avatarContainer}>
          <PulsingCircle isRecording={isRecording} />
          <View style={[styles.profileImageContainer, isRecording && styles.recording]}>
            {isRecording ? (
               <View style={styles.soundWave}>
                  <View style={[styles.bar, { height: 10 }]} />
                  <View style={[styles.bar, { height: 20 }]} />
                  <View style={[styles.bar, { height: 15 }]} />
                  <View style={[styles.bar, { height: 25 }]} />
                  <View style={[styles.bar, { height: 10 }]} />
               </View>
            ) : (
              <Image
                source={{ uri: 'https://placehold.co/80x80/E8F5E9/333?text=Bot' }}
                style={styles.profileImage}
              />
            )}
          </View>
        </View>
        <Text style={styles.botName}>김여사 봇</Text>
      </View>

      <View style={styles.controlsContainer}>
        <TouchableOpacity style={styles.iconButton}>
          <MaterialIcons name="video-call" size={32} color="#555" />
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.recordButton, isRecording && styles.recordButtonActive]}
          onPress={handleRecordButtonPress}
        >
          {isRecording ? (
            <Ionicons name="pause" size={40} color="white" />
          ) : (
            <Ionicons name="mic" size={40} color="white" />
          )}
        </TouchableOpacity>

        <TouchableOpacity style={styles.iconButton}>
          <Ionicons name="ellipsis-horizontal-circle-outline" size={32} color="#555" />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: 50,
  },
  botProfileContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  // New style for the wrapper
  avatarContainer: {
    width: 140,
    height: 140,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 20,
  },
  profileImageContainer: {
    width: 140,
    height: 140,
    borderRadius: 70,
    backgroundColor: '#E8F5E9',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 4,
    borderColor: 'rgba(0, 200, 83, 0.2)',
  },
  recording: {
    backgroundColor: '#00C853',
    borderColor: 'rgba(0, 200, 83, 0.4)',
  },
  profileImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
  },
  botName: {
    fontSize: 22,
    fontWeight: '600',
    color: '#333',
    // marginTop was removed and replaced by marginBottom in avatarContainer
  },
  controlsContainer: {
    flexDirection: 'row',
    width: '80%',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  recordButton: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#00C853',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  recordButtonActive: {
     backgroundColor: '#FF4136', // Change color when recording
  },
  iconButton: {
    padding: 10,
  },
  pulsingCircle: {
    width: 140,
    height: 140,
    borderRadius: 70,
    backgroundColor: 'rgba(0, 200, 83, 0.8)',
    position: 'absolute',
  },
  soundWave: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 30,
  },
  bar: {
    width: 4,
    backgroundColor: 'white',
    marginHorizontal: 3,
    borderRadius: 2,
  },
});

