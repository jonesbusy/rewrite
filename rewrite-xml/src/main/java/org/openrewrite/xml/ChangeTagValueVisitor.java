/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml;

import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

public class ChangeTagValueVisitor<P> extends XmlVisitor<P> {

    private final Xml.Tag scope;
    private final String value;
    private final String comment;

    public ChangeTagValueVisitor(Xml.Tag scope, String value) {
        this(scope, value, null);
    }

    public ChangeTagValueVisitor(Xml.Tag scope, String value, String comment) {
        this.scope = scope;
        this.value = value;
        this.comment = comment;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);
        if (scope.isScope(t)) {
            String prefix = "";
            String afterText = "";
            if (t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                Xml.CharData existingValue = (Xml.CharData) t.getContent().get(0);

                if (existingValue.getText().equals(value)) {
                    return tag;
                }

                // if the previous content was also character data, preserve its prefix and afterText
                prefix = existingValue.getPrefix();
                afterText = existingValue.getAfterText();
            }
            List<Content> content = new LinkedList<>();
            if (comment != null && !comment.isEmpty()) {
                doAfterVisit(new AddCommentVisitor<>(prefix, t, comment));
            }
            content.add(new Xml.CharData(randomId(), prefix, Markers.EMPTY, false, value, afterText));
            t = t.withContent(content);
        }

        return t;
    }

    private static class AddCommentVisitor<P> extends XmlVisitor<P> {
        private final String prefix;
        private final Xml.Tag target;
        private final String comment;

        public AddCommentVisitor(String prefix, Xml.Tag target, String comment) {
            this.prefix = prefix;
            this.target = target;
            this.comment = comment;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, P p) {
            List<Content> content = new LinkedList<>(tag.getContent());
            content.add(new Xml.Comment(randomId(), prefix, Markers.EMPTY, comment));
            return tag.withContent(content);
        }
    }

}
