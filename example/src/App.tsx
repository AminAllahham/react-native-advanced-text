import { FlashList, type ListRenderItemInfo } from '@shopify/flash-list';
import { useCallback } from 'react';
import { StyleSheet, View, type NativeSyntheticEvent } from 'react-native';
import { AdvancedText } from 'react-native-advanced-text';

const HIGHLIGHTED_WORDS = [
  { index: 2, highlightColor: '#FFD60A', borderRadius: 10 },
  { index: 4, highlightColor: '#6baeffb5', borderRadius: 6 },
];

type Row =
  | { kind: 'heading'; id: string; text: string }
  | { kind: 'text'; id: string; text: string };

// A handful of real, varied-length paragraphs (excerpted from "The Design
// of Everyday Things" by Don Norman) - enough to exercise wrapping across
// several lines per block without shipping a whole book's worth of JSON in
// the example.
const SAMPLE_PARAGRAPHS: string[] = [
  '"Kenneth Olsen, the engineer who founded and still runs Digital Equipment Corp., confessed at the annual meeting that he can\'t figure out how to heat a cup of coffee in the company\'s microwave oven."',
  "\"You would need an engineering degree from MIT to work this,\" someone once told me, shaking his head in puzzlement over his brand new digital watch. Well, I have an engineering degree from MIT. (Kenneth Olsen has two of them, and he can't figure out a microwave oven.) Give me a few hours and I can figure out the watch. But why should it take hours? I have talked with many people who can't use all the features of their washing machines or cameras, who can't figure out how to work a sewing machine or a video cassette recorder, who habitually turn on the wrong stove burner.",
  "Why do we put up with the frustrations of everyday objects, with objects that we can't figure out how to use, with those neat plastic wrapped packages that seem impossible to open, with doors that trap people, with washing machines and dryers that have become too confusing to use, with audio stereo television video cassette recorders that claim in their advertisements to do everything, but that make it almost impossible to do anything? The human mind is exquisitely tailored to make sense of the world. Give it the slightest clue and off it goes, providing explanation, rationalization, understanding. Consider the objects books, radios, kitchen appliances, office machines, and light switches that make up our everyday lives. Well designed objects are easy to interpret and understand. They contain visible clues to their operation.",
  'Poorly designed objects can be difficult and frustrating to use. They provide no clues or sometimes false clues. They trap the user and thwart the normal process of interpretation and understanding. Alas, poor design predominates. The result is a world filled with frustration, with objects that cannot be understood, with devices that lead to error. This book is an attempt to change things.',
  'The Frustrations of Everyday Life. If I were placed in the cockpit of a modern jet airliner, my inability to perform gracefully and smoothly would neither surprise nor bother me. But I shouldn\'t have trouble with doors and switches, water faucets and stoves. "Doors?" I can hear the reader saying, "you have trouble opening doors?" Yes. I push doors that are meant to be pulled, pull doors that should be pushed, and walk into doors that should be slid. Moreover, I see others having the same troubles unnecessary troubles. There are psychological principles that can be followed to make these things understandable and usable. Consider the door. There is not much you can do to a door: you can open it or shut it. Suppose you are in an office building, walking down a corridor. You come to a door. In which direction does it open? Should you pull or push, on the left or the right? Maybe the door slides.',
  'If so, in which direction? I have seen doors that slide up into the ceiling. A door poses only two essential questions: In which direction does it move? On which side should one work it? The answers should be given by the design, without any need for words or symbols, certainly without any need for trial and error.',
  'A friend told me of the time he got trapped in the doorway of a post office in a European city. The entrance was an imposing row of perhaps six glass swinging doors, followed immediately by a second, identical row. That\'s a standard design: it helps reduce the airflow and thus maintain the indoor temperature of the building. My friend pushed on the side of one of the leftmost pair of outer doors. It swung inward, and he entered the building. Then, before he could get to the next row of doors, he was distracted and turned around for an instant. He didn\'t realize it at the time, but he had moved slightly to the right. So when he came to the next door and pushed it, nothing happened. "Hmm," he thought, "must be locked." So he pushed the side of the adjacent door. Nothing. Puzzled, my friend decided to go outside again. He turned around and pushed against the side of a door. Nothing.',
  "He pushed the adjacent door. Nothing. The door he had just entered no longer worked. He turned around once more and tried the inside doors again. Nothing. Concern, then mild panic. He was trapped! Just then, a group of people on the other side of the entranceway (to my friend's right) passed easily through both sets of doors. My friend hurried over to follow their path. How could such a thing happen? A swinging door has two sides. One contains the supporting pillar and the hinge, the other is unsupported. To open the door, you must push on the unsupported edge. If you push on the hinge side, nothing happens. In this case, the designer aimed for beauty, not utility. No distracting lines, no visible pillars, no visible hinges. So how can the ordinary user know which side to push on?",
  'The door story illustrates one of the most important principles of design: visibility. The correct parts must be visible, and they must convey the correct message. With doors that push, the designer must provide signals that naturally indicate where to push. These need not destroy the aesthetics. Put a vertical plate on the side to be pushed, nothing on the other. Or make the supporting pillars visible. The vertical plate and supporting pillars are natural signals, naturally interpreted, without any need to be conscious of them. I call the use of natural signals natural design and elaborate on the approach throughout this book.',
  "Visibility problems come in many forms. My friend, trapped between the glass doors, suffered from a lack of clues that would indicate what part of a door should be operated. Other problems concern the mappings between what you want to do and what appears to be possible, another topic that will be expanded upon throughout the book. Consider one type of slide projector. This projector has a single button to control whether the slide tray moves forward or backward. One button to do two things? What is the mapping? How can you figure out how to control the slides? You can't. Nothing is visible to give the slightest hint.",
];

const ROWS: Row[] = [
  {
    kind: 'heading',
    id: 'h-0',
    text: 'The Psychopathology of Everyday Things',
  },
  ...SAMPLE_PARAGRAPHS.map((text, index) => ({
    kind: 'text' as const,
    id: `p-${index}`,
    text,
  })),
];

export default function App() {
  const onWordPress = useCallback((e: NativeSyntheticEvent<WordPressEvent>) => {
    console.log('Word pressed:', e.nativeEvent.word);
    console.log('Word index:', e.nativeEvent.index);
  }, []);

  const onSelection = useCallback((e: NativeSyntheticEvent<SelectionEvent>) => {
    console.log('Selected text:', e.nativeEvent.selectedText);
    console.log('Event type:', e.nativeEvent.event);
  }, []);

  const renderItem = useCallback(
    ({ item }: ListRenderItemInfo<Row>) =>
      item.kind === 'heading' ? (
        <AdvancedText
          text={item.text}
          style={styles.AdvancedText}
          fontSize={24}
          fontWeight="800"
          lineHeight={1}
          color={'#FFFFFF'}
        />
      ) : (
        <AdvancedText
          text={item.text}
          style={styles.AdvancedText}
          onWordPress={onWordPress}
          menuOptions={['Highlight', 'Copy', 'Translate']}
          onSelection={onSelection}
          highlightedWords={HIGHLIGHTED_WORDS}
          fontSize={18}
          lineHeight={1.2}
          fontWeight="normal"
          color={'#FFFFFF'}
        />
      ),
    [onWordPress, onSelection]
  );

  return (
    <FlashList
      data={ROWS}
      keyExtractor={(item) => item.id}
      getItemType={(item) => item.kind}
      renderItem={renderItem}
      contentContainerStyle={styles.content}
      ItemSeparatorComponent={ItemSeparator}
    />
  );
}

function ItemSeparator() {
  return <View style={styles.separator} />;
}

const styles = StyleSheet.create({
  content: {
    padding: 20,
    paddingVertical: 120,
  },
  separator: {
    height: 8,
  },
  AdvancedText: {
    width: '100%',
  },
});

type WordPressEvent = {
  word: string;
  index: number;
};

type SelectionEvent = {
  selectedText: string;
  event: string;
};
