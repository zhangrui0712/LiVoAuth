package idpservice.audio.pretreatment;

public class PreProcess {

    float[] afterEndPtDetection;// after endPointDetection 
    public int noOfFrames;// calculated total no of frames 
    int samplePerFrame;// how many samples in one frame 256/512
    int framedArrayLength;// how many samples in framed array
    public float[][] framedSignal;
    float[] hammingWindow;
    int samplingRate;

    /**
     * constructor, all steps are called frm here
     *
     * @param audioData      extracted PCM data
     * @param samplePerFrame how many samples in one frame,=660 << frameDuration, typically
     *                       30; samplingFreq, typically 22Khz
     */
    public PreProcess(float[] afterEndPtDetection, int samplePerFrame, int samplingRate) {
        this.samplePerFrame = samplePerFrame;
        this.samplingRate = samplingRate;
        this.afterEndPtDetection = afterEndPtDetection;

        doFraming();
        doWindowing();
    }


    /**
     * divides the whole signal into frames of samplerPerFrame ��֡
     */
    private void doFraming() {
        // calculate no of frames, for framing
        int length =0;
        try{
            length= afterEndPtDetection.length;
        }catch (NullPointerException e){

        }
        noOfFrames = 2 * length / samplePerFrame - 1;
        if (noOfFrames < 0) noOfFrames = 0;
        System.out.println("noOfFrames       " + noOfFrames + "  samplePerFrame     " + samplePerFrame + "  EPD length   " + length);
        framedSignal = new float[noOfFrames][samplePerFrame];
        for (int i = 0; i < noOfFrames; i++) {
            int startIndex = (i * samplePerFrame / 2);
            for (int j = 0; j < samplePerFrame; j++) {
                framedSignal[i][j] = afterEndPtDetection[startIndex + j];
            }
        }
    }

    /**
     * does hamming window on each frame
     */
    private void doWindowing() {
        // prepare hammingWindow
        hammingWindow = new float[samplePerFrame + 1];
        // prepare for through out the data
        for (int i = 1; i <= samplePerFrame; i++) {

            hammingWindow[i] = (float) (0.54 - 0.46 * (Math.cos(2 * Math.PI * i / samplePerFrame)));
        }
        // do windowing
        for (int i = 0; i < noOfFrames; i++) {
            for (int j = 0; j < samplePerFrame; j++) {
                framedSignal[i][j] = framedSignal[i][j] * hammingWindow[j + 1];
            }
        }
    }
}
