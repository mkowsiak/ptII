package ptolemy.actor.gt;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ptolemy.actor.gt.data.MatchResult;
import ptolemy.data.expr.ScopeExtender;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.expr.Variable;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;

public class TransformationMode extends StringParameter
implements MatchCallback {

    public TransformationMode(NamedObj container, String name)
    throws IllegalActionException, NameDuplicationException {
        super(container, name);

        for (Mode mode : Mode.values()) {
            addChoice(mode.toString());
        }
        setExpression(Mode.REPLACE_FIRST.toString());
    }

    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        TransformationMode newObject = (TransformationMode) super.clone(
                workspace);
        newObject._masterRule = null;
        newObject._random = new Random();
        newObject._workingCopy = null;
        newObject._workingCopyVersion = -1;
        return newObject;
    }

    public List<MatchResult> findAllMatches(TransformationRule workingCopy,
            CompositeEntity model) throws IllegalActionException {
        Pattern pattern = workingCopy.getPattern();
        GraphMatcher matcher = new GraphMatcher();

        matcher.setMatchCallback(this);
        _matchResults.clear();
        _collectAllMatches = true;
        matcher.match(pattern, model);

        return _matchResults;
    }

    public boolean foundMatch(GraphMatcher matcher) {
        _matchResults.add((MatchResult) matcher.getMatchResult().clone());
        return !_collectAllMatches;
    }

    public Mode getMode() {
        String modeString = getExpression();
        for (Mode mode : Mode.values()) {
            if (modeString.equals(mode.toString())) {
                return mode;
            }
        }
        return null;
    }

    public boolean isMatchOnly() {
        return getMode() == Mode.MATCH_ONLY;
    }

    public boolean transform(TransformationRule workingCopy,
    		CompositeEntity model) throws IllegalActionException {
        Pattern pattern = workingCopy.getPattern();
        GraphMatcher matcher = new GraphMatcher();
        Mode mode = getMode();

        matcher.setMatchCallback(this);
        _matchResults.clear();
        _collectAllMatches = mode != Mode.REPLACE_FIRST;
        matcher.match(pattern, model);

        if (_matchResults.isEmpty()) {
            return false;
        } else {
            try {
                switch (mode) {
                case REPLACE_FIRST:
                    MatchResult result = _matchResults.getFirst();
                    GraphTransformer.transform(workingCopy, result);
                    break;
                case REPLACE_LAST:
                    result = _matchResults.getLast();
                    GraphTransformer.transform(workingCopy, result);
                    break;
                case REPLACE_ANY:
                    result = _matchResults.get(_random.nextInt(
                            _matchResults.size()));
                    GraphTransformer.transform(workingCopy, result);
                    break;
                case REPLACE_ALL:
                    GraphTransformer.transform(workingCopy, _matchResults);
                    break;
                case MATCH_ONLY:
                    break;
                }
            } catch (TransformationException e) {
                throw new IllegalActionException("Unable to transform model.");
            }
            return true;
        }
    }

    public enum Mode {
        MATCH_ONLY {
            public String toString() {
                return "match only";
            }
        },
        REPLACE_ALL {
            public String toString() {
                return "replace all";
            }
        },
        REPLACE_ANY {
            public String toString() {
                return "replace any";
            }
        },
        REPLACE_FIRST {
            public String toString() {
                return "replace first";
            }
        },
        REPLACE_LAST {
            public String toString() {
                return "replace last";
            }
        }
    }

    public TransformationRule getWorkingCopy(TransformationRule masterRule)
    throws IllegalActionException {
        if (_masterRule != masterRule || _workingCopy == null
                || _workingCopyVersion != masterRule.workspace().getVersion()) {
            if (_workingCopy != null) {
                _workspace.remove(_workingCopy);
                _workingCopy = null;
            }
            try {
                _workingCopy = (TransformationRule) masterRule.clone(
                        _workspace);
                new WorkingCopyScopeExtender(_workingCopy, "_scopeExtender",
                        masterRule);
                _masterRule = masterRule;
                _workingCopyVersion = masterRule.workspace().getVersion();
            } catch (Exception e) {
                throw new IllegalActionException(this, e, "Cannot get a " +
                        "working copy this transformation rule.");
            }
        }
        return _workingCopy;
    }

    private boolean _collectAllMatches;

    private TransformationRule _masterRule;

    private LinkedList<MatchResult> _matchResults =
        new LinkedList<MatchResult>();

    private Random _random = new Random();

    private TransformationRule _workingCopy;

    private long _workingCopyVersion = -1;

    private Workspace _workspace = new Workspace();

    private static class WorkingCopyScopeExtender extends Attribute
    implements ScopeExtender {

        public List<?> attributeList() {
            NamedObj container = _masterRule;
            Set<?> names = VariableScope.getAllScopedVariableNames(null,
                    container);
            List<Variable> variables = new LinkedList<Variable>();
            for (Object name : names) {
                variables.add(VariableScope.getScopedVariable(null, container,
                        (String) name));
            }
            return variables;
        }

        public Attribute getAttribute(String name) {
            NamedObj container = _masterRule;
            return VariableScope.getScopedVariable(null, container, name);
        }

        WorkingCopyScopeExtender(NamedObj container, String name,
                TransformationRule masterRule) throws IllegalActionException,
                NameDuplicationException {
            super(container, name);
            _masterRule = masterRule;
            setPersistent(false);
        }

        private TransformationRule _masterRule;
    }
}
