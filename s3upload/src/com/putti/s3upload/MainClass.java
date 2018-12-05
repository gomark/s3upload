package com.putti.s3upload;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.google.api.gax.paging.Page;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Acl.User;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;


public class MainClass {
	
	private static String sourceFileName = "/Users/puttitang/Downloads/all_currencies.csv.gz";
	private static String bucketName = "puttitang-bucket-1";
	private static String objectName = "all_currencies.csv";

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		//uploadGoogleCloudStorage();

		//testSpeech2Text("/Users/puttitang/Downloads/Call1_1_30secs.wav");
		speech2TextChunk("/Users/puttitang/Downloads/Call1_1_6582081237218829519_1_79.wav");
		//createAutoMLDataset();
		
		//copyAudio("/Users/puttitang/Downloads/Call1_1_30secs_44100Khz.wav", "/Users/puttitang/Downloads/Call1_6.wav", 19, 3);
		
		System.out.println("DONE");		
	}
	
	private static void speech2TextChunk(String filename) {
		int i;
		int max=3;
		int chunkSize = 45;
		
		for (i = 0; i < max; i++) {
			System.out.println("i=" + String.valueOf(i));
			copyAudio(filename, "/Users/puttitang/Downloads/temp.wav", i*chunkSize, chunkSize);
			testSpeech2Text("/Users/puttitang/Downloads/temp.wav");
		}
	}
	
	private static void createAutoMLDataset() {
		String mlBucketName = "putti-project-vcm";
		String prefix = "Hollywood1/images/MESSI";
		String outputCsv = "/Users/puttitang/Downloads/Hollywood1.csv";
		String label = "messi";
		
				
		try {
			FileWriter fw = new FileWriter(outputCsv, true);
			BufferedWriter bw = new BufferedWriter(fw);
			
			Storage storage = StorageOptions.getDefaultInstance().getService();
			//BucketInfo bucketInfo = Bucket.of(mlBucketName);
			
			//Page<Blob> blobs = storage.list(mlBucketName);
			Page<Blob> blobs = storage.list(mlBucketName, BlobListOption.prefix(prefix));
			for (Blob blob : blobs.iterateAll()) {
				System.out.println(blob.getName());
				String line = "gs://" + mlBucketName + "/" + blob.getName() + "," + label;
				System.out.println(line);
				bw.write(line + "\n");
			}
			
			bw.close();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void copyAudio(String sourceFileName, String destinationFileName, int startSecond, int secondsToCopy) {
		AudioInputStream inputStream = null;
		AudioInputStream shortenedStream = null;
		try {
			File file = new File(sourceFileName);
			AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
			AudioFormat format = fileFormat.getFormat();
			inputStream = AudioSystem.getAudioInputStream(file);
			int bytesPerSecond = format.getFrameSize() * (int)format.getFrameRate();
			inputStream.skip(startSecond * bytesPerSecond);
			long framesOfAudioToCopy = secondsToCopy * (int)format.getFrameRate();
			shortenedStream = new AudioInputStream(inputStream, format, framesOfAudioToCopy);
			File destinationFile = new File(destinationFileName);
			AudioSystem.write(shortenedStream, fileFormat.getType(), destinationFile);
		} catch (Exception e) {
			System.out.println(e);
	    } finally {
	    	if (inputStream != null) try { inputStream.close(); } catch (Exception e) { System.out.println(e); }
	    	if (shortenedStream != null) try { shortenedStream.close(); } catch (Exception e) { System.out.println(e); }
	    }
	}	
	
	private static void testSpeech2Text(String filename) {
		try {
			SpeechClient speech = SpeechClient.create();
			Path path = Paths.get(filename);
			// /Users/puttitang/Downloads/Call1_1_30secs_44100Khz.wav
			byte[] data = Files.readAllBytes(path);
		    ByteString audioBytes = ByteString.copyFrom(data);
		    
		    RecognitionConfig config = RecognitionConfig.newBuilder()
		        .setEncoding(AudioEncoding.LINEAR16)
		        .setLanguageCode("th-TH")
		        .setSampleRateHertz(8000)
		        .build();
		    RecognitionAudio audio = RecognitionAudio.newBuilder()
		        .setContent(audioBytes)
		        .build();
		    
		    System.out.println("converting..");
		    
		    // Use blocking call to get audio transcript
		    RecognizeResponse response = speech.recognize(config, audio);
		    
		    System.out.println("resultCount=" + String.valueOf(response.getResultsCount()));
		    List<SpeechRecognitionResult> results = response.getResultsList();
		    

		    for (SpeechRecognitionResult result : results) {
		      // There can be several alternative transcripts for a given chunk of speech. Just use the
		      // first (most likely) one here.
		      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
		      
		      System.out.println("alternative_count=" + String.valueOf(result.getAlternativesCount()));
		      
		      System.out.printf("Transcription: %s%n", alternative.getTranscript());
		      System.out.println("confidence=" + String.valueOf(alternative.getConfidence()));
		      
		    }		    
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static void uploadGoogleCloudStorage() {
		System.out.println("Google Cloud Storage demo..");
		
		try {
			Storage storage = StorageOptions.getDefaultInstance().getService();		
			
			
			BlobInfo blobInfo = storage.create(BlobInfo.newBuilder(bucketName, objectName).setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllAuthenticatedUsers(), Role.READER)))).setContentEncoding("gzip").build(), new FileInputStream(sourceFileName));
			
			System.out.println("Public_Link=" + blobInfo.getMediaLink());
		} catch (Exception e) {
			e.printStackTrace();
		}
						
		
	}

}
