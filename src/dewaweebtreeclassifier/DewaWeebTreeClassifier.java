package dewaweebtreeclassifier;

import weka.core.Instances;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.supervised.instance.Resample;
import weka.classifiers.Evaluation;
import java.util.Random;
import weka.classifiers.*;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemovePercentage;

/**
 *
 * @author Ahmad
 */
public class DewaWeebTreeClassifier {

    private Instances mData;
    private String loadedDataPath;
    private Classifier classifier;
    Evaluation eval;

    /**
     *
     */
    public void printMain() {
        String dialog[] = {
            "Hai, apa yang ingin kamu lakukan?",
            "1. Load File",
            "2. Filter",
            "3. Display Attributes",
            "4. Remove Attribute",
            "5. Select Classifier",
            "6. Training",
            "7. Classify",};
        
        if (mData != null){
            System.out.println("\nData: " + loadedDataPath);
        }
        
        for (String line : dialog) {
            System.out.println(line);
        }
    }
    
    enum Classifiers {
        NONE,
        NAIVE_BAYES,
        ID3,
        C4_5,
    }
    
    enum Menu {
        NOOP,
        LOAD_FILE,
        FILTER,
        DISPLAY_ATTRIBUTES,
        REMOVE,
        SELECT_CLASSIFIER,
        TRAINING,
        CLASSIFY
    }

    Classifiers classifiers[] = {
        Classifiers.NONE,
        Classifiers.NAIVE_BAYES,
        Classifiers.ID3,
        Classifiers.C4_5
    };
    
    Menu mainMenu[] = {
        Menu.NOOP,
        Menu.LOAD_FILE,
        Menu.FILTER,
        Menu.DISPLAY_ATTRIBUTES,
        Menu.REMOVE,
        Menu.SELECT_CLASSIFIER,
        Menu.TRAINING,
        Menu.CLASSIFY
    };

    /**
     * 
     * @param path the .arff or .csv file path
     * @throws Exception 
     */
    private void loadFile(String path) throws Exception {
        // Load data
        DataSource source = new DataSource(path);
        mData = source.getDataSet();
        // setting class attribute if the data format does not provide this information
        // For example, the XRFF format saves the class attribute information as well
        if (mData.classIndex() == -1) {
            mData.setClassIndex(mData.numAttributes() - 1);
        }
    }
    
    /**
     * resample instances from the whole dataset
     * @return new dataset after resampling
     * @throws Exception 
     */
    private Instances resampleInstances() throws Exception {
        Resample sampler = new Resample();
        sampler.setInputFormat(mData);
        
        Instances newData = Resample.useFilter(mData, sampler);
        newData.setClassIndex(newData.numAttributes() - 1);
        
        return newData;
    }
    
    /**
     * 
     * @param idxAttrs the list of indexes of the attributes to be removed (from 0)
     * @param invertSelection if true, attributes in idxAttrs will be kept, instead of being removed
     * @return new dataset after attribute removal
     */
    private Instances removeAttribute(int[] idxAttrs, boolean invertSelection) throws Exception {
        Remove remove = new Remove();
        
        remove.setAttributeIndicesArray(idxAttrs);
        remove.setInvertSelection(invertSelection);
        remove.setInputFormat(mData);
        
        Instances newData = Filter.useFilter(mData, remove);
        newData.setClassIndex(newData.numAttributes() - 1);
        
        return newData;
    }
    
    /**
     * 
     * @param percent how many instances in the dataset will be processed
     * @param invertSelection invertSelection if true, percent of the dataset will be kept, instead of being removed
     * @return
     * @throws Exception 
     */
    private Instances percentageSplit(double percent, boolean invertSelection) throws Exception {
        RemovePercentage split = new RemovePercentage();
        
        split.setInvertSelection(invertSelection);
        split.setPercentage(percent);
        split.setInputFormat(mData);
        
        Instances newData = Filter.useFilter(mData, split);
        newData.setClassIndex(newData.numAttributes() - 1);
        
        return newData;
    }
    
    /**
     * Method to save an already trained classifier
     * @param path the file name and path for the saved model
     * @param tree the classifier
     * @throws Exception 
     */
    private void saveModel(String path, Classifier tree) throws Exception {
        weka.core.SerializationHelper.write(path, tree);
    }
    
    /**
     * 
     * @param path the file path where the saved model is located
     * @return the classifier loaded from the file
     * @throws Exception 
     */
    private Classifier loadModel(String path) throws Exception {
        Classifier tree = (Classifier) weka.core.SerializationHelper.read(path);
        return tree;
    }
    /**
     * 
     * @param instances 
     */
    private void displayAttributes(Instances instances){
        Enumeration attributes = instances.enumerateAttributes();
        int i=0;
        while (attributes.hasMoreElements()){
            Attribute attr = (Attribute) attributes.nextElement();
            System.out.println(String.valueOf(i) + " " + attr.toString());
            i++;
        }
    }
    /**
     * 
     */
    private void displayClassifiers(){
        System.out.println("Classifier list: ");
        System.out.println("1. Naive Bayes");
        System.out.println("2. ID3");
        System.out.println("3. C4.5");
    }
    /**
     * 
     * @param selection 
     */
    private void selectClassifier(int selection){
        Classifiers cls = this.classifiers[selection];
        switch (cls){
            case NAIVE_BAYES:
                classifier = new NaiveBayes();
                break;
            case ID3:
                classifier = new Veranda();
                break;
            case C4_5:
//                classifier = new Sujeong();
                break;
        }
    }
    
    /**
     * 
     * @param tree the model to cross validate the dataset
     * @return
     * @throws Exception 
     */
    private Evaluation tenCrossValidation(Classifier tree) throws Exception {
        Evaluation evaluation = new Evaluation(mData);
        evaluation.crossValidateModel(tree, mData, 10, new Random(1));
        return evaluation;
    }
    
    /**
     * 
     * @param tree
     * @return
     * @throws Exception 
     */
    private Evaluation percentageSplitEvaluation(Classifier tree) throws Exception {
        Random rand = new Random(1);
        Instances randData = new Instances(mData);
        randData.randomize(rand);
        
        Instances train = randData.trainCV(5, 1);
        Instances test = randData.testCV(5,1);
        
        Evaluation evaluation = new Evaluation(train);
        evaluation.evaluateModel(classifier, test);
        return evaluation;
    }
    
    /**
     * 
     * @param tree
     * @param testData
     * @return
     * @throws Exception 
     */
    private Evaluation evaluateGivenTestData(Classifier tree, Instances testData) throws Exception{
        Evaluation evaluation = new Evaluation(mData);
        evaluation.evaluateModel(classifier, testData);
        return evaluation;
    }
    
    /**
     * 
     */
    private void displayEvaluationMethods(){
        System.out.println("Choose evaluation method: ");
        System.out.println("1. 10-fold cross validation");
        System.out.println("2. 80%-20% split");
        System.out.println("3. Use additional testdata");
    }
    
    /**
     * 
     * @param selection
     * @throws Exception 
     */
    private void evaluateData(int selection) throws Exception{
        switch(selection){
            case 1:
                eval = tenCrossValidation(classifier);
                break;
            case 2:
                eval = percentageSplitEvaluation(classifier);
                break;
            case 3:
                // Load data
                System.out.print("Test data file path: ");
                Scanner sc = new Scanner(System.in);
                String testDataPath = sc.nextLine();
                DataSource source = new DataSource(testDataPath);
                Instances testData = source.getDataSet();
                
                // setting class attribute if the data format does not provide this information
                // For example, the XRFF format saves the class attribute information as well
                if (mData.classIndex() == -1) {
                    mData.setClassIndex(mData.numAttributes() - 1);
                }
                
                eval = evaluateGivenTestData(classifier, testData);
                break;
        }
    }
    
    /**
     * 
     * @param evaluation 
     */
    private void displayEvaluation(Evaluation evaluation){
        System.out.println(evaluation.toSummaryString());
    }
    
    /**
     *
     */
    public void run() {
        while (true) {
            printMain();
            Scanner sc = new Scanner(System.in);
            int choiceNum = Integer.parseInt(sc.nextLine());
            Menu chosenMenu = mainMenu[choiceNum];
            try {
                switch (chosenMenu) {
                    case LOAD_FILE:
                        System.out.print("Please input file path: ");
                        loadedDataPath = sc.nextLine();
                        this.loadFile(loadedDataPath);
                        break;
                    case FILTER:
                        // resample case: Get the dataset and re-sample it to get a new dataset
                        mData = this.resampleInstances();
                        break;
                    case DISPLAY_ATTRIBUTES:
                        displayAttributes(mData);
                        break;
                    case REMOVE:
                        // remove attributes
                        displayAttributes(mData);
                        System.out.print("Please select the attribute index: ");
                        String idxStr = sc.nextLine();
                        String[] listIdxStr = idxStr.split("\\s+");
                        int[] listIdx = new int[listIdxStr.length];
                        for (int i = 0; i < listIdx.length; i++) {
                            listIdx[i] = Integer.parseInt(listIdxStr[i]);
                        }
                        System.out.print("Invert selection? (Y/N): ");
                        String invStr = sc.nextLine();
                        boolean isInv = false;
                        if (invStr.toLowerCase().compareTo("y") == 0)
                            isInv = true;
                        
                        mData = this.removeAttribute(listIdx, isInv);
                        break;  
                    case SELECT_CLASSIFIER:
                        // Select the classifier
                        displayClassifiers();
                        System.out.print("Choose classifier: ");                        
                        int classifierSelection = Integer.parseInt(sc.nextLine());
                        selectClassifier(classifierSelection);
                    case TRAINING:
                        // Train and evaluate data
                        displayEvaluationMethods();
                        int validationMethod = Integer.parseInt(sc.nextLine());
                        evaluateData(validationMethod);
                        displayEvaluation(eval);

                        break;   
                    case CLASSIFY:
                        // Copy data
//                        Instances classified = new Instances(mData);
                        // Classify data
                        //for (int i = 0; i < mData.numInstances(); i++) {
                        //    double classifiedLabel = mData.classifyInstancemDatadata.instance(i));
//                            classified.instance(i).setClassValue(classifiedLabel);
//                        }
                        break;
                }
            } catch (Exception ex) {
                System.out.println(ex.toString());
                break;  
            }
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            DewaWeebTreeClassifier app = new DewaWeebTreeClassifier();
            app.run();
        } catch (Exception ex) {
            Logger.getLogger(DewaWeebTreeClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
