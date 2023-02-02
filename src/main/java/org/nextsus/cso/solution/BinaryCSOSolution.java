package org.nextsus.cso.solution;

import org.nextsus.cso.model.cells.Cell;
import org.uma.jmetal.solution.binarysolution.impl.DefaultBinarySolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.List;

/**
 * This defines an implementation of a binary solution. These solutions are composed of a number of
 * variables containing {@link BinarySet} objects.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class BinaryCSOSolution extends DefaultBinarySolution {

    List<Cell> currentUesToCellAssignment_ = null;
    List<Cell> previousUesToCellAssignment_ = null;

    public BinaryCSOSolution(List<Integer> bitsPerVariable, int numberOfObjectives) {
        super(bitsPerVariable, numberOfObjectives);
    }

    public BinaryCSOSolution(List<Integer> bitsPerVariable, int numberOfObjectives, int numberOfConstraints) {
        super(bitsPerVariable, numberOfObjectives, numberOfConstraints);
    }

    public BinaryCSOSolution(BinaryCSOSolution solution) {
        super(solution);
        this.currentUesToCellAssignment_ = solution.currentUesToCellAssignment_;
        this.previousUesToCellAssignment_ = solution.previousUesToCellAssignment_;
    }

    public BinaryCSOSolution(BinarySet binarySet, List<Double> objectives) {
        super(List.of(binarySet.size()), objectives.size());
        for (int i = 0; i < objectives.size(); i++) {
            objectives()[i] = objectives.get(i);
        }
        variables().set(0, binarySet);
    }

    public BinaryCSOSolution(BinarySet binarySet) {
        super(List.of(binarySet.size()), 2);
        variables().set(0, binarySet);
    }

    public void setUEsToCellAssignment(List<Cell> assignment) {
        previousUesToCellAssignment_ = currentUesToCellAssignment_;
        currentUesToCellAssignment_ = assignment;
    }

    public List<Cell> getCurrentUesToCellAssignment() {
        return currentUesToCellAssignment_;
    }

    public List<Cell> getPreviousUesToCellAssignment() {
        return previousUesToCellAssignment_;
    }

    public void forgetUEsToCellAssignment() {
        previousUesToCellAssignment_ = null;
        currentUesToCellAssignment_ = null;
    }

    @Override
    public BinaryCSOSolution copy() {
        return new BinaryCSOSolution(this);
    }
}
