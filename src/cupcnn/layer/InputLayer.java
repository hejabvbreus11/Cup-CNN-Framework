package cupcnn.layer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cupcnn.Network;
import cupcnn.data.Blob;
/*
 * InputLayer主要作用是占据第一个位置，是的反向传播的算法更容易实现
 */
import cupcnn.data.BlobParams;

public class InputLayer extends Layer{
	public static final String TYPE = "InputLayer";
	public InputLayer(Network network, BlobParams parames) {
		super(network, parames);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void prepare() {
		
	}

	@Override
	public void forward() {
	}

	@Override
	public void backward() {
	}
	
	public void setInputData(Blob input){
		Blob curData = mNetwork.getDatas().get(id);
		input.cloneTo(curData);
	}

	@Override
	public void saveModel(ObjectOutputStream out) {
		try {
			out.writeUTF(getType());
			//保存的时候，batch也就是layerParams的number总是1，因为predict的时候，因为真正使用的时候，这个batch一般都是1
			layerParams.setNumbers(1);
			out.writeObject(layerParams);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadModel(ObjectInputStream in) {
	}
}
