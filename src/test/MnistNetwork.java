package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cupcnn.Network;
import cupcnn.active.ReluActivationFunc;
import cupcnn.data.Blob;
import cupcnn.data.BlobParams;
import cupcnn.layer.ConvolutionLayer;
import cupcnn.layer.FullConnectionLayer;
import cupcnn.layer.InputLayer;
import cupcnn.layer.PoolMaxLayer;
import cupcnn.layer.PoolMeanLayer;
import cupcnn.layer.SoftMaxLayer;
import cupcnn.loss.CrossEntropyLoss;
import cupcnn.optimizer.Optimizer;
import cupcnn.optimizer.SGDMOptimizer;

public class MnistNetwork {
	Network network;
	SGDMOptimizer optimizer;
	
	private void buildConvNetwork(){
		InputLayer layer1 = new InputLayer(network,new BlobParams(network.getBatch(),1,28,28));
		network.addLayer(layer1);
		
		ConvolutionLayer conv1 = new ConvolutionLayer(network,new BlobParams(network.getBatch(),6,28,28),new BlobParams(1,6,4,4));
		conv1.setActivationFunc(new ReluActivationFunc());
		network.addLayer(conv1);
		
		PoolMaxLayer pool1 = new PoolMaxLayer(network,new BlobParams(network.getBatch(),6,14,14),new BlobParams(1,6,2,2),2,2);
		network.addLayer(pool1);
		
		ConvolutionLayer conv2 = new ConvolutionLayer(network,new BlobParams(network.getBatch(),36,14,14),new BlobParams(1,36,3,3));
		conv2.setActivationFunc(new ReluActivationFunc());
		network.addLayer(conv2);
		
		PoolMeanLayer pool2 = new PoolMeanLayer(network,new BlobParams(network.getBatch(),36,7,7),new BlobParams(1,36,2,2),2,2);
		network.addLayer(pool2);
		
		
		FullConnectionLayer fc1 = new FullConnectionLayer(network,new BlobParams(network.getBatch(),512,1,1),0.01);//dropoutRatio = 0.3
		fc1.setActivationFunc(new ReluActivationFunc());
		network.addLayer(fc1);
		

		FullConnectionLayer fc2 = new FullConnectionLayer(network,new BlobParams(network.getBatch(),64,1,1),0.01);//dropoutRatio = 0.3
		fc2.setActivationFunc(new ReluActivationFunc());
		network.addLayer(fc2);

		FullConnectionLayer fc3 = new FullConnectionLayer(network,new BlobParams(network.getBatch(),10,1,1),0.01);//dropoutRatio = 0.3
		fc3.setActivationFunc(new ReluActivationFunc());
		network.addLayer(fc3);
		
		SoftMaxLayer sflayer = new SoftMaxLayer(network,new BlobParams(network.getBatch(),10,1,1));
		network.addLayer(sflayer);
		
	}
	public void buildNetwork(int numOfTrainData){
		//首先构建神经网络对象，并设置参数
		network = new Network();
		network.setBatch(100);
		network.setLoss(new CrossEntropyLoss());
		optimizer = new SGDMOptimizer(0.1,5.0,Optimizer.GMode.L2,numOfTrainData,0.5);
		network.setOptimizer(optimizer);
		
		buildConvNetwork();

		network.prepare();
	}
	
	public List<Blob> buildBlobByImageList(List<DigitImage> imageList,int start,int batch,int channel,int height,int width){
		Blob input = new Blob(batch,channel,height,width);
		Blob label = new Blob(batch,network.getDatas().get(network.getDatas().size()-1).get3DSize(),1,1);
		label.fillValue(0);
		double[] blobData = input.getData();
		double[] labelData = label.getData();
		for(int i=start;i<(batch+start);i++){
			DigitImage img = imageList.get(i);
			byte[] imgData = img.imageData;
			assert img.imageData.length== input.get3DSize():"buildBlobByImageList -- blob size error";
			for(int j=0;j<imgData.length;j++){
				blobData[(i-start)*input.get3DSize()+j] = (imgData[j]&0xff)/128.0-1;//normalize and centerlize(-1,1)
			}
			int labelValue = img.label;
			for(int j=0;j<label.get3DSize();j++){
				if(j==labelValue){
					labelData[(i-start)*label.get3DSize()+j] = 1;
				}
			}
		}
		List<Blob> inputAndLabel = new ArrayList<Blob>();
		inputAndLabel.add(input);
		inputAndLabel.add(label);
		return inputAndLabel;
	}
	
	private int getMaxIndexInArray(double[] data){
		int maxIndex = 0;
		double maxValue = 0;
		for(int i=0;i<data.length;i++){
			if(maxValue<data[i]){
				maxValue = data[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
	
	private int[] getBatchOutputLabel(double[] data){
		int[] outLabels = new int[network.getDatas().get(network.getDatas().size()-1).getNumbers()];
		int outDataSize = network.getDatas().get(network.getDatas().size()-1).get3DSize();
		for(int n=0;n<outLabels.length;n++){
			int maxIndex = 0;
			double maxValue = 0;
			for(int i=0;i<outDataSize;i++){
				if(maxValue<data[n*outDataSize+i]){
					maxValue = data[n*outDataSize+i];
					maxIndex = i;
				}	
			}
			outLabels[n] = maxIndex;
		}
		return outLabels;
	}
	
	private void testInner(Blob input,Blob label){
		Blob output = network.predict(input);
		int[] calOutLabels = getBatchOutputLabel(output.getData());
		int[] realLabels = getBatchOutputLabel(label.getData());
		assert calOutLabels.length == realLabels.length:"network train---calOutLabels.length == realLabels.length error";
		int correctCount = 0;
		for(int kk=0;kk<calOutLabels.length;kk++){
			if(calOutLabels[kk] == realLabels[kk]){
				correctCount++;
			}
		}
		double accuracy = correctCount/(1.0*realLabels.length);
		System.out.println("accuracy is "+accuracy);
	}
	
	
	public void train(List<DigitImage> imgList,int epoes){
		System.out.println("begin train");
		int batch = network.getBatch();
		double loclaLr = optimizer.getLr();
		for(int e=0;e<epoes;e++){
			Collections.shuffle(imgList);
			for(int i=0;i<imgList.size()-batch;i+=batch){
				List<Blob> inputAndLabel = buildBlobByImageList(imgList,i,batch,1,28,28);
				double lossValue = network.train(inputAndLabel.get(0), inputAndLabel.get(1));
				
				if(i>batch && i/batch%50==0){
					System.out.print("epoe: "+e+" lossValue: "+lossValue+"  "+" lr: "+optimizer.getLr()+"  ");
					testInner(inputAndLabel.get(0), inputAndLabel.get(1));
				}
			}
			
			if(loclaLr>0.001){
				loclaLr*=0.7;
				optimizer.setLr(loclaLr);
			}
		}
	}
	

	
	public void test(List<DigitImage> imgList){
		System.out.println("begin test");
		int batch = network.getBatch();
		int correctCount = 0;
		int i = 0;
		for(i=0;i<imgList.size()-batch;i+=batch){
			List<Blob> inputAndLabel = buildBlobByImageList(imgList,i,batch,1,28,28);
			Blob output = network.predict(inputAndLabel.get(0));
			int[] calOutLabels = getBatchOutputLabel(output.getData());
			int[] realLabels = getBatchOutputLabel(inputAndLabel.get(1).getData());
			for(int kk=0;kk<calOutLabels.length;kk++){
				if(calOutLabels[kk] == realLabels[kk]){
					correctCount++;
				}
			}
		}
		
		double accuracy = correctCount/(i*1.0+1-batch);
		System.out.println("test accuracy is "+accuracy+" correctCount "+correctCount);
	}
	
	public void saveModel(String name){
		network.saveModel(name);
	}
	
	public void loadModel(String name){
		network = new Network();
		network.loadModel(name);
		network.prepare();
	}
}
